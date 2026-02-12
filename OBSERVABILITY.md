# Observability Guide

VelocityGate is built with production-grade observability in mind, utilizing the "Three Pillars": Metrics, Logging, and Tracing.

## 1. Metrics (Prometheus & Grafana)

We expose operational metrics via Spring Boot Actuator at `/actuator/prometheus`.

### Custom Metrics

The Gateway tracks specific business logic metrics:

- `gateway_requests_total{status="200", route="user-service"}`: Throughput counter.
- `gateway_ratelimit_rejected_total{api_key_id="123"}`: Rate limit hits.
- `resilience4j_circuitbreaker_state{name="user-service", state="open"}`: Circuit breaker status (Gauge: 1=Open, 0=Closed).
- `lettuce_command_latency_seconds`: Redis operation timing.

### Setup

1.  **Prometheus**: Point to `http://gateway:8080/actuator/prometheus`.
2.  **Grafana**: Import `monitoring/grafana-dashboard.json`.
3.  **Alerting**: Use `monitoring/prometheus-alerts.yml` for predefined rules (Error Rate > 1%, High Latency, etc.).

---

## 2. Structured Logging (ELK Stack)

VelocityGate uses **Logback** with **LogstashEncoder** to output JSON logs. This is critical for centralized logging systems (ELK, Splunk, Datadog) to parse fields automatically.

### Configuration (`logback-spring.xml`)

Logs are formatted as JSON lines:

```json
{
  "@timestamp": "2023-10-24T10:00:00.123+00:00",
  "level": "INFO",
  "message": "Incoming request",
  "logger_name": "com.gateway.apigateway.filter.RequestLoggingFilter",
  "app_name": "api-gateway",
  "traceId": "65345d8b7d903f21",
  "spanId": "4c697850811e582d",
  "method": "GET",
  "uri": "/api/v1/users",
  "status": 200,
  "duration_ms": 15
}
```

### Best Practices

- **Correlation IDs**: Automatically injected via `MDC` (Mapped Diagnostic Context) and Micrometer Tracing.
- **No Sensitive Data**: Passwords, API Keys, and PII are redacted or hashed before logging.
- **Levels**:
  - `INFO`: Normal business events (Startup, Config loaded).
  - `WARN`: Recoverable issues (Rate limit exceeded, Fallback triggered).
  - `ERROR`: System failures (Redis down, DB unreachable).

---

## 3. Distributed Tracing (OpenTelemetry)

We use **Micrometer Tracing** with **OpenTelemetry** bridge to trace requests across microservices.

### Configuration

Add to `application.yml`:

```yaml
management:
  tracing:
    sampling:
      probability: 1.0 # Sample 100% of requests (lower this for production)
  zipkin:
    tracing:
      endpoint: "http://jaeger:9411/api/v2/spans"
```

### Architecture

1.  **Gateway**: Generates `traceId` and `spanId` for incoming request.
2.  **Propagation**: Injects `b3` or `traceparent` headers into downstream requests.
3.  **Backend**: Services pick up the headers and continue the trace.
4.  **Visualize**: Use Jaeger UI (`http://localhost:16686`) to see the full waterfall.

### Use Case: Debugging Latency

If a request takes 500ms:

- Gateway Span: 500ms
  - Redis Auth Check: 5ms
  - Rate Limit Check: 2ms
  - **User Service Call**: 490ms (Problem identified here!)

---

## 4. Alerting Rules

Key alerts configured in `prometheus-alerts.yml`:

| Alert                    | Condition                            | Severity | Action                             |
| :----------------------- | :----------------------------------- | :------- | :--------------------------------- |
| **High Error Rate**      | > 1% of requests are 5xx (5m window) | CRITICAL | PagerDuty / On-Call                |
| **Circuit Breaker Open** | State = 1 for > 1m                   | CRITICAL | Check downstream service health    |
| **High Latency**         | P99 > 100ms (5m window)              | WARNING  | Investigate performance regression |
| **Redis Flapping**       | > 5 failures/min                     | CRITICAL | Check Redis HA / Network           |
