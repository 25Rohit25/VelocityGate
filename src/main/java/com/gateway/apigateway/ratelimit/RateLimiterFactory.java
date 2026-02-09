package com.gateway.apigateway.ratelimit;

import com.gateway.apigateway.model.enums.RateLimitAlgorithm;
import com.gateway.apigateway.ratelimit.strategy.FixedWindowRateLimiter;
import com.gateway.apigateway.ratelimit.strategy.LeakyBucketRateLimiter;
import com.gateway.apigateway.ratelimit.strategy.RateLimiterStrategy;
import com.gateway.apigateway.ratelimit.strategy.SlidingWindowRateLimiter;
import com.gateway.apigateway.ratelimit.strategy.TokenBucketRateLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class RateLimiterFactory {
    private final TokenBucketRateLimiter tokenBucketRateLimiter;
    private final SlidingWindowRateLimiter slidingWindowRateLimiter;
    private final FixedWindowRateLimiter fixedWindowRateLimiter;
    private final LeakyBucketRateLimiter leakyBucketRateLimiter;

    public RateLimiterStrategy getStrategy(RateLimitAlgorithm algorithm) {
        if (algorithm == null) {
            return tokenBucketRateLimiter;
        }
        return switch (algorithm) {
            case TOKEN_BUCKET -> tokenBucketRateLimiter;
            case SLIDING_WINDOW -> slidingWindowRateLimiter;
            case FIXED_WINDOW -> fixedWindowRateLimiter;
            case LEAKY_BUCKET -> leakyBucketRateLimiter;
        };
    }
}
