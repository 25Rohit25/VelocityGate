package com.gateway.apigateway.service;

import com.gateway.apigateway.model.dto.AnalyticsRequest;
import com.gateway.apigateway.model.dto.AnalyticsResponse;
import com.gateway.apigateway.model.entity.RequestLog;
import com.gateway.apigateway.repository.RequestLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private RequestLogRepository requestLogRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void getUsageStatistics_ShouldCalculateCorrectly() {
        // Given
        AnalyticsRequest request = new AnalyticsRequest();
        request.setStartDate(LocalDateTime.now().minusDays(1));
        request.setEndDate(LocalDateTime.now());

        RequestLog log1 = RequestLog.builder().statusCode(200).responseTimeMs(50L).build();
        RequestLog log2 = RequestLog.builder().statusCode(500).responseTimeMs(150L).build();
        RequestLog log3 = RequestLog.builder().statusCode(429).wasRateLimited(true).responseTimeMs(10L).build();
        List<RequestLog> logs = Arrays.asList(log1, log2, log3);

        when(requestLogRepository.findByTimestampBetween(any(), any())).thenReturn(logs);

        // When
        AnalyticsResponse response = analyticsService.getUsageStatistics(request);

        // Then
        assertEquals(3, response.getTotalRequests());
        assertEquals(1, response.getSuccessCount()); // Only 200 is success < 400
        assertEquals(2, response.getFailedCount()); // 500 and 429 are failures/errors
        assertEquals(1, response.getRateLimitedCount());
        assertEquals(70.0, response.getAverageLatency(), 0.01);
    }
}
