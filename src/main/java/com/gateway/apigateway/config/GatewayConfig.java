package com.gateway.apigateway.config;

import com.gateway.apigateway.filter.AuthenticationFilter;
import com.gateway.apigateway.filter.LoadBalancerFilter;
import com.gateway.apigateway.filter.LoggingFilter;
import com.gateway.apigateway.filter.RateLimitFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder,
            AuthenticationFilter authFilter,
            RateLimitFilter rateLimitFilter,
            LoggingFilter loggingFilter,
            LoadBalancerFilter loadBalancerFilter) {
        return builder.routes()
                .route("user_service_route", r -> r.path("/api/v1/users/**")
                        .filters(f -> f.filter(authFilter)
                                .filter(rateLimitFilter)
                                .filter(loadBalancerFilter) // Custom LB logic if needed
                                .filter(loggingFilter))
                        .uri("lb://user-service")) // Use Spring Cloud LoadBalancer

                .route("order_service_route", r -> r.path("/api/v1/orders/**")
                        .filters(f -> f.filter(authFilter)
                                .filter(rateLimitFilter)
                                .filter(loggingFilter))
                        .uri("lb://order-service"))

                .build();
    }
}
