package com.gateway.apigateway.ratelimit.strategy;

import com.gateway.apigateway.model.entity.RateLimitConfig;
import reactor.core.publisher.Mono;

public interface RateLimiterStrategy {
    Mono<Boolean> allowRequest(String key, RateLimitConfig config);

    Mono<Long> getRemaining(String key, RateLimitConfig config);

    Mono<Long> getResetTime(String key, RateLimitConfig config);
}
