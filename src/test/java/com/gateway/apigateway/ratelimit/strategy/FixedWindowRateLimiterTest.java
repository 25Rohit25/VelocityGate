package com.gateway.apigateway.ratelimit.strategy;

import com.gateway.apigateway.model.entity.RateLimitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FixedWindowRateLimiterTest {

    @Mock
    private ReactiveStringRedisTemplate redisTemplate;

    @Mock
    private ReactiveValueOperations<String, String> valueOperations;

    private FixedWindowRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        rateLimiter = new FixedWindowRateLimiter(redisTemplate);
    }

    @Test
    void shouldAllowRequestWhenCountIsBelowLimit() {
        // Arrange
        RateLimitConfig config = new RateLimitConfig();
        config.setRequestsPerMinute(10);

        when(valueOperations.increment(anyString())).thenReturn(Mono.just(5L));

        // Act & Assert
        StepVerifier.create(rateLimiter.allowRequest("test-key", config))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void shouldDenyRequestWhenCountExceedsLimit() {
        // Arrange
        RateLimitConfig config = new RateLimitConfig();
        config.setRequestsPerMinute(10);

        when(valueOperations.increment(anyString())).thenReturn(Mono.just(11L));

        // Act & Assert
        StepVerifier.create(rateLimiter.allowRequest("test-key", config))
                .expectNext(false)
                .verifyComplete();
    }

    @Test
    void shouldSetExpirationOnFirstRequest() {
        // Arrange
        RateLimitConfig config = new RateLimitConfig();
        config.setRequestsPerMinute(10);

        when(valueOperations.increment(anyString())).thenReturn(Mono.just(1L));
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Mono.just(true));

        // Act & Assert
        StepVerifier.create(rateLimiter.allowRequest("test-key", config))
                .expectNext(true)
                .verifyComplete();
    }
}
