package com.gateway.apigateway.repository;

import com.gateway.apigateway.model.entity.CircuitBreakerState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CircuitBreakerStateRepository extends JpaRepository<CircuitBreakerState, Long> {
    Optional<CircuitBreakerState> findByServiceName(String serviceName);
}
