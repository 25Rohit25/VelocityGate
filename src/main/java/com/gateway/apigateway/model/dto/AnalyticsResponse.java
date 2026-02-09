package com.gateway.apigateway.model.dto;

import lombok.Builder;
import lombok.Data;
import java.util.Map;

@Data
@Builder
public class AnalyticsResponse {
    private long totalRequests;
    private long successCount;
    private long failedCount;
    private long rateLimitedCount;
    private double averageLatency;
    private Map<String, Long> requestsByService;
}
