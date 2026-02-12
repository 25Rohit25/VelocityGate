# Failure Modes & Resilience Strategy

VelocityGate is designed to be resilient in the face of infrastructure failures. This document details our strategies for handling critical component failures, circuit breaker configurations, and graceful degradation.

## 1. Failure Scenarios Matrix

The following table outlines how VelocityGate behaves under various failure conditions:

| Failure Scenario         | Detection Mechanism                 | System Response                                                                       | Recovery Strategy                                                | Observability Signals                                                        |
| :----------------------- | :---------------------------------- | :------------------------------------------------------------------------------------ | :--------------------------------------------------------------- | :--------------------------------------------------------------------------- |
| **Redis Unavailable**    | Connection Exception / Timeout > 2s | **Fail-Open**: Logging error, _allowing_ request to proceed without rate limiting.    | Auto-reconnect via Lettuce driver.                               | `log.error("Redis connection failed")`, Spike in `rate_limit.bypass` metric. |
| **Backend Service Down** | HTTP 5xx / Timeout                  | **Circuit Breaker Open**: Fast-fail subsequent requests with fallback response.       | **Half-Open Algorithm**: Periodically test backend connectivity. | Prometheus `resilience4j_circuitbreaker_state{state="open"} = 1`.            |
| **Redis Memory Full**    | OOM Error from Redis                | **Fail-Open**: Similar to unavailability. Rate limits temporarily disabled.           | Redis Eviction Policy (LRU) or Scale-up.                         | `redis_memory_used_bytes` > Threshold.                                       |
| **Cluster Partition**    | Redis Topology Refresh              | **Fail-Open or Retry**: Client attempts to connect to majority partition.             | Topology refresh on connection restore.                          | `lettuce.reconnect.count` increase.                                          |
| **Gateway Crash**        | K8s Liveness Probe                  | **Pod Restart**: K8s replaces the dead pod.                                           | New pod initializes and joins cluster.                           | K8s `restart_count` increase.                                                |
| **DB Unavailable**       | Connection Timeout                  | **Cached Config**: Serve rate limits from local cache (if enabled) or Default limits. | Connection Pool retry.                                           | `hikaricp_connections_pending` spike.                                        |

---

## 2. Circuit Breaker Configuration (Resilience4j)

We use Resilience4j to prevent cascading failures. When a downstream service (e.g., User Service) fails, the Gateway stops sending requests to give it time to recover.

### Key Settings (`application.yml`)

```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        registerHealthIndicator: true
        # Window size for calculating error rate
        slidingWindowSize: 100
        # Min calls before calculating error rate
        minimumNumberOfCalls: 20
        # If 50% of requests fail, OPEN the circuit
        failureRateThreshold: 50
        # Wait 60s before trying again (HALF-OPEN)
        waitDurationInOpenState: 60s
        # Allow 10 test requests in HALF-OPEN state
        permittedNumberOfCallsInHalfOpenState: 10
        # Automatically move from OPEN -> HALF-OPEN
        automaticTransitionFromOpenToHalfOpenEnabled: true
        # Treat calls > 3s as failures
        slowCallDurationThreshold: 3000ms
        slowCallRateThreshold: 50
```

### State Transitions

1.  **CLOSED**: Normal operation. Requests pass through.
2.  **OPEN**: Failure threshold reached. All requests fail fast (or go to fallback) without hitting backend.
3.  **HALF-OPEN**: After `waitDurationInOpenState`, allow configured number of probe requests.
    - Success: Transition to **CLOSED**.
    - Failure: Return to **OPEN**.

---

## 3. Graceful Degradation Strategy

When systems fail, VelocityGate degrades functionality rather than crashing completely.

### Priority Levels

1.  **Critical (P0)**: Authentication & Routing. (Must work; if DB down, use cached keys).
2.  **High (P1)**: Rate Limiting. (If Redis down, bypass temporarily).
3.  **Medium (P2)**: Analytics/Logging. (If buffer full, drop logs).

### Fallback Implementation

**Custom Fallback Controller:**

```java
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @GetMapping("/user-service")
    public ResponseEntity<FallBackResponse> userServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new FallBackResponse(
                "User Service is currently unavailable.",
                "Please try again later.",
                Instant.now()
            ));
    }

    // Configurable "Partial Mode" response
    @GetMapping("/product-service")
    public ResponseEntity<ProductResponse> productFallback() {
        // Return cached "Best Seller" list instead of personalized feed
        return ResponseEntity.ok(cachedProductService.getGenericProducts());
    }
}
```

---

## 4. Resilience Code Patterns

### A. Redis Retry Logic (Lettuce)

Spring Data Redis (Lettuce) handles connection resilience automatically, but we customize the topology refresh.

```java
@Bean
public LettuceConnectionFactory redisConnectionFactory() {
    RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(nodes);

    ClusterClientOptions clusterClientOptions = ClusterClientOptions.builder()
        .topologyRefreshOptions(ClusterTopologyRefreshOptions.builder()
            .enablePeriodicRefresh(Duration.ofMinutes(10))
            .enableAllAdaptiveRefreshTriggers()
            .build())
        .build();

    LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
        .clientOptions(clusterClientOptions)
        .commandTimeout(Duration.ofSeconds(2)) // Fail fast
        .build();

    return new LettuceConnectionFactory(clusterConfig, clientConfig);
}
```

### B. Fail-Open Rate Limiter

Ensures Redis errors don't block legitimate traffic.

```java
public Mono<Boolean> isAllowed(String key, RateLimitConfig config) {
    return redisTemplate.execute(script, keys, args)
        .onErrorResume(e -> {
            log.error("Redis rate limit check failed for key: {}. Error: {}", key, e.getMessage());
            // FAIL-OPEN: Return true (allowed) if Redis is down
            return Mono.just(true);
        })
        .map(result -> result == 1L);
}
```
