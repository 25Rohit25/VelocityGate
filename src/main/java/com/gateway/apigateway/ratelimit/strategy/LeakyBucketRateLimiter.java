package com.gateway.apigateway.ratelimit.strategy;

import com.gateway.apigateway.model.entity.RateLimitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class LeakyBucketRateLimiter implements RateLimiterStrategy {

    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<Boolean> allowRequest(String key, RateLimitConfig config) {
        log.warn("LeakyBucketRateLimiter bypassed (stubbed). Key: {}", key);
        return Mono.just(true);
    }

    @Override
    public Mono<Long> getRemaining(String key, RateLimitConfig config) {
        // ... implementation similar to others
        return Mono.just(0L); // Placeholder
    }

    @Override
    public Mono<Long> getResetTime(String key, RateLimitConfig config) {
        return Mono.just(0L); // Placeholder
    }

    private long getLimit(RateLimitConfig config) {
        if (config.getRequestsPerSecond() != null)
            return config.getRequestsPerSecond();
        return 10;
    }

    private long getProcessingRate(RateLimitConfig config) {
        // Simplify for this implementation, assume 1 request per second process rate or
        // based on config
        return 1;
    }
}
