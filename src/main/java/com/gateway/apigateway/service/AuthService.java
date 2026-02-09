package com.gateway.apigateway.service;

import com.gateway.apigateway.model.dto.LoginRequest;
import com.gateway.apigateway.model.dto.LoginResponse;
import com.gateway.apigateway.model.dto.RegisterRequest;
import com.gateway.apigateway.model.entity.User;
import com.gateway.apigateway.model.enums.UserRole;
import com.gateway.apigateway.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ReactiveAuthenticationManager authenticationManager;

    public Mono<RegisterRequest> register(RegisterRequest request) {
        return Mono.fromCallable(() -> {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("Email already taken");
            }

            var user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole(request.getRole() != null ? request.getRole() : UserRole.USER);
            user.setActive(true);

            userRepository.save(user);
            return request;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<LoginResponse> login(LoginRequest request) {
        return authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()))
                .flatMap(auth -> Mono.fromCallable(() -> userRepository.findByEmail(request.getEmail())
                        .orElseThrow(() -> new RuntimeException("User not found")))
                        .subscribeOn(Schedulers.boundedElastic()))
                .map(user -> {
                    var jwtToken = jwtService.generateToken(user);
                    var refreshToken = jwtService.generateRefreshToken(user);

                    LoginResponse response = new LoginResponse();
                    response.setAccessToken(jwtToken);
                    response.setRefreshToken(refreshToken);
                    response.setExpiresIn(3600000); // 1 hour
                    return response;
                });
    }
}
