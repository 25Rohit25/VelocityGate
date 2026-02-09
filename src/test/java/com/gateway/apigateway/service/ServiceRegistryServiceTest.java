package com.gateway.apigateway.service;

import com.gateway.apigateway.model.entity.Service;
import com.gateway.apigateway.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceRegistryServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @InjectMocks
    private ServiceRegistryService serviceRegistryService;

    @Test
    void registerService_ShouldSaveService() {
        // Given
        Service service = new Service();
        service.setName("test-service");
        service.setUrl("http://localhost:8081");
        service.setStatus("UP");

        when(serviceRepository.save(any(Service.class))).thenReturn(service);

        // When
        Service result = serviceRegistryService.registerService(service);

        // Then
        assertNotNull(result);
        assertEquals("test-service", result.getName());
        verify(serviceRepository).save(service);
    }

    @Test
    void deregisterService_ShouldDeleteService() {
        // Given
        Long serviceId = 1L;

        // When
        serviceRegistryService.deregisterService(serviceId);

        // Then
        verify(serviceRepository).deleteById(serviceId);
    }
}
