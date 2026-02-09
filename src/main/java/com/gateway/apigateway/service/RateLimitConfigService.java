package com.gateway.apigateway.service;

import com.gateway.apigateway.model.entity.RateLimitConfig;
import com.gateway.apigateway.repository.RateLimitConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimitConfigService {

    private final RateLimitConfigRepository configRepository;

    @Cacheable(value = "rate_limit_config", key = "#apiKeyId + '_' + #path")
    public RateLimitConfig getConfig(Long apiKeyId, String path) {
        // Simple exact match logic for now.
        // In production, this would do pattern matching (AntPathMatcher).
        // For efficiency, we might load all configs for an API key and match in memory.
        return configRepository.findByApiKey_IdAndEndpointPattern(apiKeyId, path)
                .orElseGet(() -> configRepository.findByApiKey_IdAndEndpointPattern(apiKeyId, "/**")
                        .orElse(null));
    }

    @CacheEvict(value = "rate_limit_config", allEntries = true)
    public RateLimitConfig updateConfig(RateLimitConfig config) {
        return configRepository.save(config);
    }
}
