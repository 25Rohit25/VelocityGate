package com.gateway.apigateway.filter;

import com.gateway.apigateway.model.entity.RateLimitConfig;
import com.gateway.apigateway.model.enums.RateLimitAlgorithm;
import com.gateway.apigateway.ratelimit.RateLimiterFactory;
import com.gateway.apigateway.ratelimit.strategy.RateLimiterStrategy;
import com.gateway.apigateway.service.RateLimitConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimitConfigService rateLimitConfigService;

    @Mock
    private RateLimiterFactory rateLimiterFactory;

    @Mock
    private RateLimiterStrategy rateLimiterStrategy;

    @Mock
    private GatewayFilterChain filterChain;

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter(rateLimitConfigService, rateLimiterFactory);
    }

    @Test
    void filter_ShouldAllowRequest_WhenRateLimitNotExceeded() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test")
                .header("X-Api-Key-Id", "100")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        RateLimitConfig config = new RateLimitConfig();
        config.setAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET);
        config.setRequestsPerSecond(10);

        when(rateLimitConfigService.getConfig(100L, "/api/v1/test")).thenReturn(config);
        when(rateLimiterFactory.getStrategy(RateLimitAlgorithm.TOKEN_BUCKET)).thenReturn(rateLimiterStrategy);
        when(rateLimiterStrategy.allowRequest(anyString(), eq(config))).thenReturn(Mono.just(true));
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = rateLimitFilter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result).verifyComplete();
        verify(filterChain).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_ShouldRejectRequest_WhenRateLimitExceeded() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/test")
                .header("X-Api-Key-Id", "100")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        RateLimitConfig config = new RateLimitConfig();
        config.setAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET);
        config.setRequestsPerSecond(10);

        when(rateLimitConfigService.getConfig(100L, "/api/v1/test")).thenReturn(config);
        when(rateLimiterFactory.getStrategy(RateLimitAlgorithm.TOKEN_BUCKET)).thenReturn(rateLimiterStrategy);
        when(rateLimiterStrategy.allowRequest(anyString(), eq(config))).thenReturn(Mono.just(false));

        // When
        Mono<Void> result = rateLimitFilter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any(ServerWebExchange.class));
        assertTrue(exchange.getResponse().getHeaders().containsKey("Retry-After"));
    }

    @Test
    void filter_ShouldSkip_WhenNoApiKeyIdHeader() {
        // Given (simulating a request that bypassed auth or is public, so no
        // X-Api-Key-Id)
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/public").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = rateLimitFilter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result).verifyComplete();
        verify(filterChain).filter(any(ServerWebExchange.class));
        verify(rateLimitConfigService, never()).getConfig(anyLong(), anyString());
    }
}
