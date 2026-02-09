package com.gateway.apigateway.filter;

import com.gateway.apigateway.exception.InvalidApiKeyException;
import com.gateway.apigateway.service.ApiKeyService;
import com.gateway.apigateway.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFilter implements GatewayFilter, Ordered {

    private final ApiKeyService apiKeyService;
    private static final String API_KEY_HEADER = "X-API-Key";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String apiKey = exchange.getRequest().getHeaders().getFirst(API_KEY_HEADER);

        if (apiKey == null || apiKey.isEmpty()) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String hashedKey = HashUtil.hashApiKey(apiKey);

        // Blocking call wrapped in reactive flow? ideally apiKeyService should be
        // reactive or return Mono
        // Since our service uses standard JPA (blocking), we need to be careful.
        // For a true high-performance gateway, we'd use R2DBC.
        // Here we'll wrap the blocking call in Mono.fromCallable.

        return Mono.fromCallable(() -> apiKeyService.validateApiKey(hashedKey))
                .flatMap(optionalKey -> {
                    if (optionalKey.isPresent()) {
                        // Add user/key context to headers for downstream
                        exchange.getRequest().mutate()
                                .header("X-User-Id", String.valueOf(optionalKey.get().getUser().getId()))
                                .header("X-Api-Key-Id", String.valueOf(optionalKey.get().getId()))
                                .build();

                        // Async update last used
                        Mono.fromRunnable(() -> apiKeyService.updateLastUsed(optionalKey.get().getId())).subscribe();

                        return chain.filter(exchange);
                    } else {
                        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                        return exchange.getResponse().setComplete();
                    }
                })
                .onErrorResume(e -> {
                    log.error("Auth error", e);
                    exchange.getResponse().setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return exchange.getResponse().setComplete();
                });
    }

    @Override
    public int getOrder() {
        return -100; // High priority, run before rate limiting
    }
}
