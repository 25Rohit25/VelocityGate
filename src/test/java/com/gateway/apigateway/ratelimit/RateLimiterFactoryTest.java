package com.gateway.apigateway.ratelimit;

import com.gateway.apigateway.model.enums.RateLimitAlgorithm;
import com.gateway.apigateway.ratelimit.strategy.FixedWindowRateLimiter;
import com.gateway.apigateway.ratelimit.strategy.LeakyBucketRateLimiter;
import com.gateway.apigateway.ratelimit.strategy.RateLimiterStrategy;
import com.gateway.apigateway.ratelimit.strategy.SlidingWindowRateLimiter;
import com.gateway.apigateway.ratelimit.strategy.TokenBucketRateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class RateLimiterFactoryTest {

    @Mock
    private TokenBucketRateLimiter tokenBucketRateLimiter;
    @Mock
    private SlidingWindowRateLimiter slidingWindowRateLimiter;
    @Mock
    private FixedWindowRateLimiter fixedWindowRateLimiter;
    @Mock
    private LeakyBucketRateLimiter leakyBucketRateLimiter;

    private RateLimiterFactory factory;

    @BeforeEach
    void setUp() {
        factory = new RateLimiterFactory(
                tokenBucketRateLimiter,
                slidingWindowRateLimiter,
                fixedWindowRateLimiter,
                leakyBucketRateLimiter
        );
    }

    @Test
    void getStrategy_ShouldReturnTokenBucket_WhenAlgorithmIsTokenBucket() {
        RateLimiterStrategy strategy = factory.getStrategy(RateLimitAlgorithm.TOKEN_BUCKET);
        assertEquals(tokenBucketRateLimiter, strategy);
    }

    @Test
    void getStrategy_ShouldReturnSlidingWindow_WhenAlgorithmIsSlidingWindow() {
        RateLimiterStrategy strategy = factory.getStrategy(RateLimitAlgorithm.SLIDING_WINDOW);
        assertEquals(slidingWindowRateLimiter, strategy);
    }

    @Test
    void getStrategy_ShouldReturnFixedWindow_WhenAlgorithmIsFixedWindow() {
        RateLimiterStrategy strategy = factory.getStrategy(RateLimitAlgorithm.FIXED_WINDOW);
        assertEquals(fixedWindowRateLimiter, strategy);
    }

    @Test
    void getStrategy_ShouldReturnLeakyBucket_WhenAlgorithmIsLeakyBucket() {
        RateLimiterStrategy strategy = factory.getStrategy(RateLimitAlgorithm.LEAKY_BUCKET);
        assertEquals(leakyBucketRateLimiter, strategy);
    }

    @Test
    void getStrategy_ShouldReturnTokenBucket_WhenAlgorithmIsNull() {
        RateLimiterStrategy strategy = factory.getStrategy(null);
        assertEquals(tokenBucketRateLimiter, strategy);
    }
}
