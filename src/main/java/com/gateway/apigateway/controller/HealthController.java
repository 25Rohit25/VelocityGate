package com.gateway.apigateway.controller;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    @GetMapping
    public Mono<ResponseEntity<Map<String, String>>> checkHealth() {
        // Custom lightweight health check for load balancers
        return Mono.just(ResponseEntity.ok(Map.of(
                "status", "UP",
                "timestamp", String.valueOf(System.currentTimeMillis()),
                "component", "api-gateway")));
    }

    @GetMapping("/detailed")
    public Mono<ResponseEntity<Map<String, Object>>> detailedHealth() {
        // In a real app, this could aggregate status from DB, Redis, and critical
        // downstream services
        return Mono.just(ResponseEntity.ok(Map.of(
                "status", "UP",
                "database", "UP", // Placeholder
                "redis", "UP" // Placeholder
        )));
    }
}
