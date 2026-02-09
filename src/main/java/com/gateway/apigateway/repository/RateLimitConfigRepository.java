package com.gateway.apigateway.repository;

import com.gateway.apigateway.model.entity.RateLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RateLimitConfigRepository extends JpaRepository<RateLimitConfig, Long> {
    Optional<RateLimitConfig> findByApiKey_IdAndEndpointPattern(Long apiKeyId, String endpointPattern);

    List<RateLimitConfig> findByApiKey_Id(Long apiKeyId);
}
