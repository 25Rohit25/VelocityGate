package com.gateway.apigateway.controller;

import com.gateway.apigateway.model.entity.Service;
import com.gateway.apigateway.model.entity.User;
import com.gateway.apigateway.repository.UserRepository;
import com.gateway.apigateway.service.ServiceRegistryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {

    private final ServiceRegistryService serviceRegistryService;
    private final UserRepository userRepository;

    // --- Service Registry Management ---

    @PostMapping("/services")
    public ResponseEntity<Service> registerService(@RequestBody Service service) {
        return ResponseEntity.ok(serviceRegistryService.registerService(service));
    }

    @GetMapping("/services")
    public ResponseEntity<List<Service>> getAllServices() {
        return ResponseEntity.ok(serviceRegistryService.getAllServices());
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<Void> deregisterService(@PathVariable Long id) {
        serviceRegistryService.deregisterService(id);
        return ResponseEntity.noContent().build();
    }

    // --- User Management ---

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PutMapping("/users/{id}/toggle-active")
    public ResponseEntity<Void> toggleUserActive(@PathVariable Long id) {
        userRepository.findById(id).ifPresent(user -> {
            user.setActive(!user.isActive());
            userRepository.save(user);
        });
        return ResponseEntity.ok().build();
    }
}
