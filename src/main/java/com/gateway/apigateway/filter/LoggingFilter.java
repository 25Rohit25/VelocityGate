package com.gateway.apigateway.filter;

import com.gateway.apigateway.model.entity.RequestLog;
import com.gateway.apigateway.repository.RequestLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoggingFilter implements GatewayFilter, Ordered {

    private final RequestLogRepository requestLogRepository;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        long startTime = System.currentTimeMillis();

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            logRequest(exchange, duration);
        }));
    }

    private void logRequest(ServerWebExchange exchange, long duration) {
        try {
            String apiKeyHash = exchange.getRequest().getHeaders().getFirst("X-API-Key"); // Or hash it again if needed
            // Actually we might want the masked key or ID

            RequestLog requestLog = RequestLog.builder()
                    .timestamp(LocalDateTime.now())
                    .method(exchange.getRequest().getMethod().name())
                    .path(exchange.getRequest().getPath().value())
                    .statusCode(exchange.getResponse().getStatusCode() != null
                            ? exchange.getResponse().getStatusCode().value()
                            : 500)
                    .responseTimeMs(duration)
                    .clientIp(exchange.getRequest().getRemoteAddress() != null
                            ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                            : "unknown")
                    // .apiKeyHash(apiKeyHash) // Handle hashing or ID storage
                    .build();

            // Fire and forget, don't block response
            Mono.fromRunnable(() -> requestLogRepository.save(requestLog))
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();

        } catch (Exception e) {
            log.error("Error logging request", e);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
