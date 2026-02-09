package com.gateway.apigateway.service;

import com.gateway.apigateway.model.entity.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class HealthCheckService {

    private final ServiceRegistryService serviceRegistryService;
    private final WebClient.Builder webClientBuilder;

    @Scheduled(fixedRate = 30000) // Check every 30 seconds
    public void checkServicesHealth() {
        log.info("Starting scheduled health check for downstream services...");
        serviceRegistryService.getAllServices().forEach(this::checkHealth);
    }

    private void checkHealth(Service service) {
        String healthUrl = service.getUrl() + "/actuator/health"; // Assumed convention

        webClientBuilder.build()
                .get()
                .uri(healthUrl)
                .retrieve()
                .toBodilessEntity()
                .timeout(Duration.ofSeconds(5))
                .subscribe(
                        response -> {
                            if (response.getStatusCode().is2xxSuccessful()) {
                                serviceRegistryService.updateServiceStatus(service.getId(), "UP");
                            } else {
                                serviceRegistryService.updateServiceStatus(service.getId(), "DOWN");
                            }
                        },
                        error -> {
                            log.warn("Health check failed for service: {} ({})", service.getName(), error.getMessage());
                            serviceRegistryService.updateServiceStatus(service.getId(), "DOWN");
                        });
    }
}
