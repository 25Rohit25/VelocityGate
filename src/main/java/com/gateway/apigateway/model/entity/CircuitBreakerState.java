package com.gateway.apigateway.model.entity;

import com.gateway.apigateway.circuitbreaker.CircuitState;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "circuit_breaker_states")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CircuitBreakerState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CircuitState state;

    private Integer failureCount;

    private LocalDateTime lastFailureTime;

    private LocalDateTime lastStateChangeTime;
}
