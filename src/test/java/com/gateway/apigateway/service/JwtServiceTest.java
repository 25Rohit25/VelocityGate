package com.gateway.apigateway.service;

import com.gateway.apigateway.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        // Set values injected via @Value
        ReflectionTestUtils.setField(jwtService, "secretKey",
                "5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437");
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1 hour
        ReflectionTestUtils.setField(jwtService, "refreshExpiration", 86400000L); // 1 day
    }

    @Test
    void generateToken_ShouldCreateValidToken() {
        // Given
        User user = new User();
        user.setEmail("test@example.com");

        // When
        String token = jwtService.generateToken(user);

        // Then
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token, user));
        assertEquals("test@example.com", jwtService.extractUsername(token));
    }
}
