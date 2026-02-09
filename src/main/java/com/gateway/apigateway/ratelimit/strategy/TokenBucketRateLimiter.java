package com.gateway.apigateway.ratelimit.strategy;

import com.gateway.apigateway.model.entity.RateLimitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenBucketRateLimiter implements RateLimiterStrategy {

    private final ReactiveStringRedisTemplate redisTemplate;

    // Lua script logic removed for stubbing
    // private static final String LUA_SCRIPT = ...
    // private final RedisScript<List<Long>> script = ...

    @Override
    public Mono<Boolean> allowRequest(String key, RateLimitConfig config) {
        log.warn("TokenBucketRateLimiter bypassed (stubbed). Key: {}", key);
        return Mono.just(true);
    }

    @Override
    public Mono<Long> getRemaining(String key, RateLimitConfig config) {
        return Mono.just(10L);
    }

    @Override
    public Mono<Long> getResetTime(String key, RateLimitConfig config) {
        return Mono.just(0L);
    }

    private Mono<List<Long>> executeScript(String key, RateLimitConfig config) {
        return Mono.just(List.of(1L, 10L));
    }

    private Mono<List<Long>> executeScript(String key, RateLimitConfig config, int requestedTokens) {
        return Mono.just(List.of(1L, 10L));
    }

    private double calculateRefillRate(RateLimitConfig config) {
        if (config.getRequestsPerSecond() != null)
            return config.getRequestsPerSecond();
        if (config.getRequestsPerMinute() != null)
            return config.getRequestsPerMinute() / 60.0;
        if (config.getRequestsPerHour() != null)
            return config.getRequestsPerHour() / 3600.0;
        if (config.getRequestsPerDay() != null)
            return config.getRequestsPerDay() / 86400.0;
        return 1.0; // Default fallback
    }
}
