package com.gateway.apigateway.model.dto;

import com.gateway.apigateway.model.enums.ApiKeyTier;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class ApiKeyRequest {
    private String name;
    private ApiKeyTier tier;
    private Long userId; // For admin creation

    public String getName() {
        return name;
    }

    public ApiKeyTier getTier() {
        return tier;
    }

    public Long getUserId() {
        return userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTier(ApiKeyTier tier) {
        this.tier = tier;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
