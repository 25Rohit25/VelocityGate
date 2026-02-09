package com.gateway.apigateway.ratelimit.strategy;

import com.gateway.apigateway.model.entity.RateLimitConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class FixedWindowRateLimiter implements RateLimiterStrategy {

    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<Boolean> allowRequest(String key, RateLimitConfig config) {
        long windowSize = getWindowSizeSeconds(config);
        long currentTime = Instant.now().getEpochSecond();
        long windowStart = (currentTime / windowSize) * windowSize;
        String redisKey = "rate_limit:fixed:" + key + ":" + windowStart;
        long limit = getLimit(config);

        return redisTemplate.opsForValue().increment(redisKey)
                .flatMap(count -> {
                    if (count == 1) {
                        return redisTemplate.expire(redisKey, java.time.Duration.ofSeconds(windowSize))
                                .thenReturn(true);
                    }
                    return Mono.just(count <= limit);
                });
    }

    @Override
    public Mono<Long> getRemaining(String key, RateLimitConfig config) {
        long windowSize = getWindowSizeSeconds(config);
        long currentTime = Instant.now().getEpochSecond();
        long windowStart = (currentTime / windowSize) * windowSize;
        String redisKey = "rate_limit:fixed:" + key + ":" + windowStart;
        long limit = getLimit(config);

        return redisTemplate.opsForValue().get(redisKey)
                .map(Long::parseLong)
                .defaultIfEmpty(0L)
                .map(count -> Math.max(0, limit - count));
    }

    @Override
    public Mono<Long> getResetTime(String key, RateLimitConfig config) {
        long windowSize = getWindowSizeSeconds(config);
        long currentTime = Instant.now().getEpochSecond();
        long windowStart = (currentTime / windowSize) * windowSize;
        long windowEnd = windowStart + windowSize;
        return Mono.just(windowEnd - currentTime);
    }

    private long getWindowSizeSeconds(RateLimitConfig config) {
        if (config.getRequestsPerSecond() != null)
            return 1;
        if (config.getRequestsPerMinute() != null)
            return 60;
        if (config.getRequestsPerHour() != null)
            return 3600;
        if (config.getRequestsPerDay() != null)
            return 86400;
        return 60;
    }

    private long getLimit(RateLimitConfig config) {
        if (config.getRequestsPerSecond() != null)
            return config.getRequestsPerSecond();
        if (config.getRequestsPerMinute() != null)
            return config.getRequestsPerMinute();
        if (config.getRequestsPerHour() != null)
            return config.getRequestsPerHour();
        if (config.getRequestsPerDay() != null)
            return config.getRequestsPerDay();
        return 10;
    }
}
