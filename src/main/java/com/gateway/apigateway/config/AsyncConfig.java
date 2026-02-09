package com.gateway.apigateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Default ThreadPoolTaskExecutor is usually sufficient for simple async tasks
    // Can customize if needed for high throughput logging
}
