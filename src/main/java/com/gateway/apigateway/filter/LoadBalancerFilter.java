package com.gateway.apigateway.filter;

import com.gateway.apigateway.model.entity.Service;
import com.gateway.apigateway.model.enums.LoadBalancerType;
import com.gateway.apigateway.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoadBalancerFilter implements GatewayFilter, Ordered {

    private final ServiceRepository serviceRepository;
    private final LoadBalancerClient loadBalancerClient; // Spring Cloud LoadBalancer

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI url = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        if (url == null || !"lb".equals(url.getScheme())) {
            // Not a load-balanced route request, or already resolved
            return chain.filter(exchange);
        }

        // This is a simplified custom logic demonstration.
        // Usually, Spring Cloud Gateway handles 'lb://service-name' automatically if
        // configured correctly.
        // However, if we want to enforce specific strategies stored in DB:

        String serviceName = url.getHost();

        // We could look up custom strategy from DB per service
        // Service serviceConfig =
        // serviceRepository.findByName(serviceName).orElse(null);
        // LoadBalancerType strategy = serviceConfig != null ?
        // serviceConfig.getStrategy() : LoadBalancerType.ROUND_ROBIN;

        // Delegation to Spring Cloud LoadBalancer which does Round Robin by default
        ServiceInstance instance = loadBalancerClient.choose(serviceName);

        if (instance == null) {
            throw new RuntimeException("Unable to find instance for " + serviceName);
        }

        URI uri = instance.getUri();
        URI requestUrl = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        // Reconstruct URI... simplified, normally Spring handles this intricacies

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return 10100; // LoadBalancerClientFilter is usually 10100
    }
}
