package com.gateway.apigateway.security;

import com.gateway.apigateway.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private String secretKey;

    @BeforeEach
    void setUp() {
        Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        secretKey = Base64.getEncoder().encodeToString(key.getEncoded());

        // Inject properties
        ReflectionTestUtils.setField(jwtService, "secretKey", secretKey);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 3600000L); // 1 hour
    }

    @Test
    void shouldValidateCorrectToken() {
        String token = Jwts.builder()
                .setSubject("user@example.com")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 10000))
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKey)))
                .compact();

        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("user@example.com");
    }

    @Test
    void shouldDetectExpiredToken() {
        String token = Jwts.builder()
                .setSubject("user@example.com")
                .setIssuedAt(new Date(System.currentTimeMillis() - 20000))
                .setExpiration(new Date(System.currentTimeMillis() - 10000)) // expired
                .signWith(Keys.hmacShaKeyFor(Base64.getDecoder().decode(secretKey)))
                .compact();

        try {
            jwtService.isTokenValid(token, null);
            assertFalse(true, "Should have thrown ExpiredJwtException");
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            assertTrue(true, "Correctly caught ExpiredJwtException");
        }
    }
}
