package com.gateway.apigateway.filter;

import com.gateway.apigateway.model.entity.ApiKey;
import com.gateway.apigateway.model.entity.User;
import com.gateway.apigateway.service.ApiKeyService;
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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @Mock
    private ApiKeyService apiKeyService;

    @Mock
    private GatewayFilterChain filterChain;

    private AuthenticationFilter authenticationFilter;

    @BeforeEach
    void setUp() {
        authenticationFilter = new AuthenticationFilter(apiKeyService);
    }

    @Test
    void filter_ShouldAllowRequest_WhenApiKeyIsValid() {
        // Given
        String apiKey = "sk_live_validkey";
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("X-API-Key", apiKey)
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        User user = new User();
        user.setId(1L);

        ApiKey keyEntity = new ApiKey();
        keyEntity.setId(100L);
        keyEntity.setUser(user);

        when(apiKeyService.validateApiKey(anyString())).thenReturn(Optional.of(keyEntity));
        when(filterChain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        // When
        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result).verifyComplete();
        verify(filterChain).filter(any(ServerWebExchange.class));
        assertEquals("1", exchange.getRequest().getHeaders().getFirst("X-User-Id"));
    }

    @Test
    void filter_ShouldRejectRequest_WhenApiKeyIsMissing() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/test").build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        // When
        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        verify(filterChain, never()).filter(any(ServerWebExchange.class));
    }

    @Test
    void filter_ShouldRejectRequest_WhenApiKeyIsInvalid() {
        // Given
        MockServerHttpRequest request = MockServerHttpRequest.get("/test")
                .header("X-API-Key", "invalid_key")
                .build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(apiKeyService.validateApiKey(anyString())).thenReturn(Optional.empty());

        // When
        Mono<Void> result = authenticationFilter.filter(exchange, filterChain);

        // Then
        StepVerifier.create(result).verifyComplete();
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }
}
