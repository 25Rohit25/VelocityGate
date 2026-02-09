package com.gateway.apigateway.repository;

import com.gateway.apigateway.model.entity.RequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {
    List<RequestLog> findByApiKeyHashAndTimestampBetween(String apiKeyHash, LocalDateTime start, LocalDateTime end);

    List<RequestLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
}
