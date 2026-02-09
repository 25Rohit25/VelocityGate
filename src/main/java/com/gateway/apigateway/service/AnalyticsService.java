package com.gateway.apigateway.service;

import com.gateway.apigateway.model.dto.AnalyticsRequest;
import com.gateway.apigateway.model.dto.AnalyticsResponse;
import com.gateway.apigateway.model.entity.RequestLog;
import com.gateway.apigateway.repository.RequestLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final RequestLogRepository requestLogRepository;

    @Transactional(readOnly = true)
    public AnalyticsResponse getUsageStatistics(AnalyticsRequest request) {
        LocalDateTime start = request.getStartDate() != null ? request.getStartDate()
                : LocalDateTime.now().minusDays(1);
        LocalDateTime end = request.getEndDate() != null ? request.getEndDate() : LocalDateTime.now();

        List<RequestLog> logs;
        if (request.getApiKey() != null) {
            // Usually we'd hash the API key or lookup the hash
            // For simplicity here assume request.getApiKey is the hash or we'd hash it
            logs = requestLogRepository.findByApiKeyHashAndTimestampBetween(request.getApiKey(), start, end);
        } else {
            logs = requestLogRepository.findByTimestampBetween(start, end);
        }

        long total = logs.size();
        long success = logs.stream().filter(l -> l.getStatusCode() != null && l.getStatusCode() < 400).count();
        long failed = total - success;
        long rateLimited = logs.stream().filter(RequestLog::isWasRateLimited).count();
        double avgLatency = logs.stream()
                .filter(l -> l.getResponseTimeMs() != null)
                .mapToLong(RequestLog::getResponseTimeMs)
                .average()
                .orElse(0.0);

        Map<String, Long> byService = logs.stream()
                .filter(l -> l.getDownstreamService() != null)
                .collect(Collectors.groupingBy(RequestLog::getDownstreamService, Collectors.counting()));

        return AnalyticsResponse.builder()
                .totalRequests(total)
                .successCount(success)
                .failedCount(failed)
                .rateLimitedCount(rateLimited)
                .averageLatency(avgLatency)
                .requestsByService(byService)
                .build();
    }
}
