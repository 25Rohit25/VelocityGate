package com.gateway.apigateway.filter;

import com.gateway.apigateway.model.entity.RateLimitConfig;
import com.gateway.apigateway.ratelimit.RateLimiterFactory;
import com.gateway.apigateway.ratelimit.strategy.RateLimiterStrategy;
import com.gateway.apigateway.service.RateLimitConfigService;
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
public class RateLimitFilter implements GatewayFilter, Ordered {

    private final RateLimitConfigService rateLimitConfigService;
    private final RateLimiterFactory rateLimiterFactory;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String apiKeyIdStr = exchange.getRequest().getHeaders().getFirst("X-Api-Key-Id");

        if (apiKeyIdStr == null) {
            // Should have been set by AuthFilter, or this is a public endpoint
            return chain.filter(exchange);
        }

        Long apiKeyId = Long.parseLong(apiKeyIdStr);
        String path = exchange.getRequest().getPath().value();

        // Blocking call wrapped. In production use reactive DB driver.
        return Mono.fromCallable(() -> rateLimitConfigService.getConfig(apiKeyId, path))
                .flatMap(config -> {
                    if (config == null) {
                        return chain.filter(exchange);
                    }

                    RateLimiterStrategy strategy = rateLimiterFactory.getStrategy(config.getAlgorithm());
                    String key = apiKeyId + ":" + path; // Unique per key + path

                    return strategy.allowRequest(key, config)
                            .flatMap(allowed -> {
                                if (allowed) {
                                    return chain.filter(exchange);
                                } else {
                                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                                    // Add headers
                                    exchange.getResponse().getHeaders().add("X-RateLimit-Limit",
                                            String.valueOf(config.getRequestsPerSecond()));
                                    exchange.getResponse().getHeaders().add("Retry-After", "1"); // Simplified
                                    return exchange.getResponse().setComplete();
                                }
                            });
                });
    }

    @Override
    public int getOrder() {
        return -50; // After Auth (-100), before others
    }
}
