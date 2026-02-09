package com.gateway.apigateway.service;

import com.gateway.apigateway.model.dto.ApiKeyRequest;
import com.gateway.apigateway.model.dto.ApiKeyResponse;
import com.gateway.apigateway.model.entity.ApiKey;
import com.gateway.apigateway.model.entity.User;
import com.gateway.apigateway.model.enums.ApiKeyTier;
import com.gateway.apigateway.repository.ApiKeyRepository;
import com.gateway.apigateway.repository.RateLimitConfigRepository;
import com.gateway.apigateway.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RateLimitConfigRepository rateLimitConfigRepository;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setActive(true);
    }

    @Test
    void generateApiKey_ShouldGenerateAndSaveKey() {
        // Given
        ApiKeyRequest request = new ApiKeyRequest();
        request.setName("Test Key");
        request.setTier(ApiKeyTier.FREE);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(invocation -> {
            ApiKey key = invocation.getArgument(0);
            key.setId(100L);
            return key;
        });

        // When
        ApiKeyResponse response = apiKeyService.generateApiKey(request, "test@example.com");

        // Then
        assertNotNull(response);
        assertNotNull(response.getKey());
        assertTrue(response.getKey().startsWith("sk_live_"));
        assertEquals("Test Key", response.getName());
        assertEquals(ApiKeyTier.FREE, response.getTier());

        verify(apiKeyRepository).save(any(ApiKey.class));
        verify(rateLimitConfigRepository).save(any());
    }

    @Test
    void validateApiKey_ShouldReturnKey_WhenActive() {
        // Given
        String hashedKey = "somehashedvalue";
        ApiKey apiKey = new ApiKey();
        apiKey.setApiKeyHash(hashedKey);
        apiKey.setActive(true);

        when(apiKeyRepository.findByApiKeyHash(hashedKey)).thenReturn(Optional.of(apiKey));

        // When
        Optional<ApiKey> result = apiKeyService.validateApiKey(hashedKey);

        // Then
        assertTrue(result.isPresent());
        assertEquals(hashedKey, result.get().getApiKeyHash());
    }

    @Test
    void revokeApiKey_ShouldDeactivateKey() {
        // Given
        Long keyId = 1L;
        ApiKey apiKey = new ApiKey();
        apiKey.setId(keyId);
        apiKey.setActive(true);
        
        when(apiKeyRepository.findById(keyId)).thenReturn(Optional.of(apiKey));

        // When
        apiKeyService.revokeApiKey(keyId);

        // Then
        verify(apiKeyRepository).save(any(ApiKey.class));
        assertFalse(apiKey.isActive());
    }
}
