package com.gateway.apigateway.model.dto;

import com.gateway.apigateway.model.enums.ApiKeyTier;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class ApiKeyResponse {
    private String id;
    private String key; // Only returned once on creation
    private String maskedKey;
    private String name;
    private ApiKeyTier tier;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setMaskedKey(String maskedKey) {
        this.maskedKey = maskedKey;
    }

    public String getMaskedKey() {
        return maskedKey;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setTier(ApiKeyTier tier) {
        this.tier = tier;
    }

    public ApiKeyTier getTier() {
        return tier;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
}
