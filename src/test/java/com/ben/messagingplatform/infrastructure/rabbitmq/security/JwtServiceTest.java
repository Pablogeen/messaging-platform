package com.ben.messagingplatform.infrastructure.rabbitmq.security;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = Base64.getEncoder()
            .encodeToString(
                    "01234567890123456789012345678901"
                            .getBytes(StandardCharsets.UTF_8)
            );

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 900000);
    }

    @Test
    void shouldGenerateAndValidateToken() {
        String token = jwtService.generateToken(1L, "john@example.com");

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
    }

    @Test
    void shouldExtractUserIdFromToken() {
        String token = jwtService.generateToken(1L, "john@example.com");

        Long userId = jwtService.extractUserId(token);

        assertEquals(1L, userId);
    }

    @Test
    void shouldExtractEmailFromToken() {
        String token = jwtService.generateToken(1L, "john@example.com");

        String email = jwtService.extractEmail(token);

        assertEquals("john@example.com", email);
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtService.generateToken(1L, "john@example.com");

        String tamperedToken = token.substring(0, token.length() - 2) + "ab";

        assertFalse(jwtService.isTokenValid(tamperedToken));
    }

    @Test
    void shouldRejectExpiredToken() throws InterruptedException {
        JwtService shortLivedJwtService = new JwtService(SECRET, 1);

        String token = shortLivedJwtService.generateToken(
                1L, "john@example.com"
        );

        Thread.sleep(1100);

        assertFalse(shortLivedJwtService.isTokenValid(token));
    }
}
