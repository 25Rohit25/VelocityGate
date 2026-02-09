package com.gateway.apigateway.controller;

import com.gateway.apigateway.model.dto.AnalyticsRequest;
import com.gateway.apigateway.model.dto.AnalyticsResponse;
import com.gateway.apigateway.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/usage")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<AnalyticsResponse> getUsage(@RequestBody AnalyticsRequest request) {
        return ResponseEntity.ok(analyticsService.getUsageStatistics(request));
    }
}
