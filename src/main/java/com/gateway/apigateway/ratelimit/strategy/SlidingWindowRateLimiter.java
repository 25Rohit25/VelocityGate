package com.gateway.apigateway.ratelimit.strategy;

import com.gateway.apigateway.model.entity.RateLimitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.redis.core.script.ReactiveScriptExecutor;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlidingWindowRateLimiter implements RateLimiterStrategy {

    private final ReactiveRedisTemplate<String, String> redisTemplate;

    @Override
    public Mono<Boolean> allowRequest(String key, RateLimitConfig config) {
        log.warn("SlidingWindowRateLimiter bypassed (stubbed). Key: {}", key);
        return Mono.just(true);
    }

    @Override
    public Mono<Long> getRemaining(String key, RateLimitConfig config) {
        return Mono.just(100L); // Stub
    }

    @Override
    public Mono<Long> getResetTime(String key, RateLimitConfig config) {
        return Mono.just(0L); // Stub
    }
}
