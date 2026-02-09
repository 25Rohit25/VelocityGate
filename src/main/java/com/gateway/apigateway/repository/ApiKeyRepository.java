package com.gateway.apigateway.repository;

import com.gateway.apigateway.model.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByApiKeyHash(String apiKeyHash);

    List<ApiKey> findByUserId(Long userId);

    boolean existsByApiKeyHash(String apiKeyHash);
}
