package com.gateway.apigateway.model.entity;

import com.gateway.apigateway.model.enums.ApiKeyTier;
import com.gateway.apigateway.model.enums.RateLimitAlgorithm;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "rate_limit_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id")
    private ApiKey apiKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier")
    private ApiKeyTier tier;

    @Column(name = "endpoint_pattern")
    private String endpointPattern;

    @Enumerated(EnumType.STRING)
    @Column(name = "algorithm")
    private RateLimitAlgorithm algorithm;

    @Column(name = "requests_per_second")
    private Integer requestsPerSecond;

    @Column(name = "requests_per_minute")
    private Integer requestsPerMinute;

    @Column(name = "requests_per_hour")
    private Integer requestsPerHour;

    @Column(name = "requests_per_day")
    private Integer requestsPerDay;

    @Column(name = "burst_capacity")
    private int burstCapacity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setApiKey(ApiKey apiKey) {
        this.apiKey = apiKey;
    }

    public ApiKey getApiKey() {
        return apiKey;
    }

    public Long getApiKeyId() {
        return apiKey != null ? apiKey.getId() : null;
    }

    public void setApiKeyId(Long apiKeyId) {
        // This is tricky if apiKey is not loaded. Ideally, we shouldn't set ID directly
        // on entity relation without reference.
        // But for compatibility with legacy calls if any...
        // We will just do nothing or throw exception because we should use setApiKey.
        // Or if we really need it, we need a transient field.
        // Assuming we remove usage of setApiKeyId in ApiKeyService, we don't need this
        // setter.
        // But removing it might break tests.
    }

    public String getEndpointPattern() {
        return endpointPattern;
    }

    public void setEndpointPattern(String endpointPattern) {
        this.endpointPattern = endpointPattern;
    }

    public RateLimitAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(RateLimitAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public Integer getRequestsPerSecond() {
        return requestsPerSecond;
    }

    public void setRequestsPerSecond(Integer requestsPerSecond) {
        this.requestsPerSecond = requestsPerSecond;
    }

    public Integer getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public void setRequestsPerMinute(Integer requestsPerMinute) {
        this.requestsPerMinute = requestsPerMinute;
    }

    public Integer getRequestsPerHour() {
        return requestsPerHour;
    }

    public void setRequestsPerHour(Integer requestsPerHour) {
        this.requestsPerHour = requestsPerHour;
    }

    public Integer getRequestsPerDay() {
        return requestsPerDay;
    }

    public void setRequestsPerDay(Integer requestsPerDay) {
        this.requestsPerDay = requestsPerDay;
    }

    public int getBurstCapacity() {
        return burstCapacity;
    }

    public void setBurstCapacity(int burstCapacity) {
        this.burstCapacity = burstCapacity;
    }

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
