package com.gateway.apigateway.integration;

import com.gateway.apigateway.AbstractIntegrationTest;
import com.gateway.apigateway.model.entity.ApiKey;
import com.gateway.apigateway.repository.ApiKeyRepository;
import com.gateway.apigateway.util.HashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

class RateLimitIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    private WebTestClient webClient;

    @BeforeEach
    void setup() {
        webClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void shouldReturn429WhenLimitExceeded() {
        // Prepare API Key
        String rawKey = "test-key-123";
        ApiKey key = new ApiKey();
        key.setApiKeyHash(HashUtil.hashApiKey(rawKey));
        key.setActive(true);
        apiKeyRepository.save(key);

        // Assume default limit is 10 RPS for Free Tier (configured in init SQL or
        // Service)

        // Fire 15 requests
        for (int i = 0; i < 15; i++) {
            webClient.get().uri("/api/v1/resource")
                    .header("X-API-Key", rawKey)
                    .exchange();
        }

        // Verify next request is blocked
        webClient.get().uri("/api/v1/resource")
                .header("X-API-Key", rawKey)
                .exchange()
                .expectStatus().isEqualTo(429);
    }
}
