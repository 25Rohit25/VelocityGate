package com.gateway.apigateway.service;

import com.gateway.apigateway.model.entity.ApiKey;
import com.gateway.apigateway.model.entity.RateLimitConfig;
import com.gateway.apigateway.repository.RateLimitConfigRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitConfigServiceTest {

        @Mock
        private RateLimitConfigRepository configRepository;

        @InjectMocks
        private RateLimitConfigService service;

        @Test
        void getConfig_ShouldReturnSpecificConfig_WhenExists() {
                // Given
                Long apiKeyId = 1L;
                String path = "/api/v1/test";
                RateLimitConfig config = new RateLimitConfig();
                config.setEndpointPattern(path);
                config.setRequestsPerSecond(100);

                when(configRepository.findByApiKey_IdAndEndpointPattern(apiKeyId, path))
                                .thenReturn(Optional.of(config));

                // When
                RateLimitConfig result = service.getConfig(apiKeyId, path);

                // Then
                assertNotNull(result);
                assertEquals(100, result.getRequestsPerSecond());
        }

        @Test
        void getConfig_ShouldReturnDefaultConfig_WhenSpecificNotFound() {
                // Given
                Long apiKeyId = 1L;
                String path = "/api/v1/test";
                RateLimitConfig defaultConfig = new RateLimitConfig();
                defaultConfig.setEndpointPattern("/**");
                defaultConfig.setRequestsPerSecond(10);

                when(configRepository.findByApiKey_IdAndEndpointPattern(apiKeyId, path))
                                .thenReturn(Optional.empty());
                when(configRepository.findByApiKey_IdAndEndpointPattern(apiKeyId, "/**"))
                                .thenReturn(Optional.of(defaultConfig));

                // When
                RateLimitConfig result = service.getConfig(apiKeyId, path);

                // Then
                assertNotNull(result);
                assertEquals(10, result.getRequestsPerSecond());
        }
}
