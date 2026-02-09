package com.gateway.apigateway.service;

import com.gateway.apigateway.model.entity.Service;
import com.gateway.apigateway.repository.ServiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRegistryService {

    private final ServiceRepository serviceRepository;

    @Transactional
    public Service registerService(Service service) {
        log.info("Registering service: {}", service.getName());
        return serviceRepository.save(service);
    }

    @Transactional
    public void deregisterService(Long id) {
        serviceRepository.deleteById(id);
    }

    public List<Service> getAllServices() {
        return serviceRepository.findAll();
    }

    public Optional<Service> getServiceByName(String name) {
        return serviceRepository.findByName(name);
    }

    @Transactional
    public void updateServiceStatus(Long id, String status) {
        serviceRepository.findById(id).ifPresent(service -> {
            service.setStatus(status);
            service.setLastHealthCheck(LocalDateTime.now());
            serviceRepository.save(service);
        });
    }
}
