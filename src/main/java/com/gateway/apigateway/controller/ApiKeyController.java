package com.gateway.apigateway.controller;

import com.gateway.apigateway.model.dto.ApiKeyRequest;
import com.gateway.apigateway.model.dto.ApiKeyResponse;
import com.gateway.apigateway.model.entity.RateLimitConfig;
import com.gateway.apigateway.service.ApiKeyService;
import com.gateway.apigateway.service.RateLimitConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/keys")
@RequiredArgsConstructor
public class ApiKeyController {

    private final ApiKeyService apiKeyService;
    private final RateLimitConfigService rateLimitConfigService;

    @PostMapping
    public ResponseEntity<ApiKeyResponse> createApiKey(@RequestBody ApiKeyRequest request, Principal principal) {
        return ResponseEntity.ok(apiKeyService.generateApiKey(request, principal.getName()));
    }

    // @GetMapping
    // public ResponseEntity<List<ApiKeyResponse>> getMyKeys(Principal principal) {
    // // Implementation needed to get user ID from principal
    // return ResponseEntity.ok(List.of());
    // }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revokeKey(@PathVariable Long id) {
        apiKeyService.revokeApiKey(id);
        return ResponseEntity.noContent().build();
    }
}
