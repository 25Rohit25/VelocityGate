package com.gateway.apigateway.ratelimit;

import com.gateway.apigateway.model.entity.RateLimitConfig;
import com.gateway.apigateway.ratelimit.strategy.TokenBucketRateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenBucketRateLimiterTest {

        @Mock
        private ReactiveStringRedisTemplate redisTemplate;

        @InjectMocks
        private TokenBucketRateLimiter rateLimiter;

        @Test
        void shouldAllowRequestWhenTokensAvailable() {
                // Given
                RateLimitConfig config = RateLimitConfig.builder()
                                .requestsPerSecond(10)
                                .burstCapacity(10)
                                .build();

                // When & Then - implementation is currently stubbed to always return true
                StepVerifier.create(rateLimiter.allowRequest("test-key", config))
                                .expectNext(true)
                                .verifyComplete();
        }
}
