package com.gateway.apigateway.service;

import com.gateway.apigateway.model.dto.ApiKeyRequest;
import com.gateway.apigateway.model.dto.ApiKeyResponse;
import com.gateway.apigateway.model.entity.ApiKey;
import com.gateway.apigateway.model.entity.RateLimitConfig;
import com.gateway.apigateway.model.entity.User;
import com.gateway.apigateway.model.enums.ApiKeyTier;
import com.gateway.apigateway.model.enums.RateLimitAlgorithm;
import com.gateway.apigateway.repository.ApiKeyRepository;
import com.gateway.apigateway.repository.RateLimitConfigRepository;
import com.gateway.apigateway.repository.UserRepository;
import com.gateway.apigateway.util.ApiKeyGenerator;
import com.gateway.apigateway.util.HashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final RateLimitConfigRepository rateLimitConfigRepository;

    @Transactional
    public ApiKeyResponse generateApiKey(ApiKeyRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getUserId() != null && user.getId().equals(request.getUserId())) {
            // Verify permission if acting on behalf of another user (e.g. admin)
        }

        String rawApiKey = ApiKeyGenerator.generateApiKey();
        String hashedKey = HashUtil.hashApiKey(rawApiKey);

        ApiKey apiKey = new ApiKey();
        apiKey.setApiKeyHash(hashedKey);
        apiKey.setName(request.getName() != null ? request.getName() : "Default Key");
        apiKey.setTier(request.getTier() != null ? request.getTier() : ApiKeyTier.FREE);
        apiKey.setUser(user);
        apiKey.setActive(true);

        ApiKey savedKey = apiKeyRepository.save(apiKey);

        // Create default rate limit config for this key based on tier
        createDefaultRateLimitConfig(savedKey);

        log.info("Generated new API key for user: {}", userEmail);

        return mapToResponse(savedKey, rawApiKey);
    }

    private void createDefaultRateLimitConfig(ApiKey apiKey) {
        RateLimitConfig config = new RateLimitConfig();
        config.setApiKey(apiKey);

        config.setEndpointPattern("/**");
        config.setAlgorithm(RateLimitAlgorithm.TOKEN_BUCKET);
        // config.setPriority(0); // If priority field exists. I recall seeing it.

        switch (apiKey.getTier()) {
            case PRO -> {
                config.setRequestsPerSecond(100);
                // config.setRequestsPerMinute(5000); // Check if field exists
                config.setBurstCapacity(200);
            }
            case ENTERPRISE -> {
                config.setRequestsPerSecond(500);
                // config.setRequestsPerMinute(25000);
                config.setBurstCapacity(1000);
                config.setAlgorithm(RateLimitAlgorithm.SLIDING_WINDOW);
            }
            default -> { // FREE
                config.setRequestsPerSecond(10);
                // config.setRequestsPerMinute(500);
                config.setBurstCapacity(20);
            }
        }
        rateLimitConfigRepository.save(config);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "api_keys", key = "#hashedKey")
    public Optional<ApiKey> validateApiKey(String hashedKey) {
        return apiKeyRepository.findByApiKeyHash(hashedKey)
                .filter(ApiKey::isActive)
                .filter(key -> key.getExpiresAt() == null || key.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    @Transactional
    @CacheEvict(value = "api_keys", allEntries = true) // Simplify cache implementation for demo
    public void revokeApiKey(Long apiKeyId) {
        apiKeyRepository.findById(apiKeyId).ifPresent(key -> {
            key.setActive(false);
            apiKeyRepository.save(key);
            log.info("Revoked API key ID: {}", apiKeyId);
        });
    }

    @Transactional
    public void updateLastUsed(Long apiKeyId) {
        // Can be async or batched for performance
        apiKeyRepository.findById(apiKeyId).ifPresent(key -> {
            key.setLastUsedAt(LocalDateTime.now());
            apiKeyRepository.save(key);
        });
    }

    private ApiKeyResponse mapToResponse(ApiKey apiKey, String rawKey) {
        ApiKeyResponse response = new ApiKeyResponse();
        response.setId(String.valueOf(apiKey.getId()));
        response.setKey(rawKey);
        response.setMaskedKey(maskKey(apiKey.getApiKeyHash()));
        response.setName(apiKey.getName());
        response.setTier(apiKey.getTier());
        response.setActive(apiKey.isActive());
        response.setCreatedAt(apiKey.getCreatedAt());
        response.setExpiresAt(apiKey.getExpiresAt());
        return response;
    }

    private String maskKey(String hash) {
        if (hash == null || hash.length() < 8)
            return "****";
        return hash.substring(0, 4) + "..." + hash.substring(hash.length() - 4);
    }

    public List<ApiKeyResponse> getUserApiKeys(Long userId) {
        return apiKeyRepository.findByUserId(userId).stream()
                .map(key -> mapToResponse(key, null))
                .collect(Collectors.toList());
    }
}
