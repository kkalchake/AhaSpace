package com.kkalchake.enlightenment.util;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-must-be-32-chars-long");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
    }

    @Test
    void generateToken_shouldCreateValidToken() {
        String token = jwtUtil.generateToken("john");
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token, "john"));
    }

    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        String token = jwtUtil.generateToken("jane");
        String username = jwtUtil.extractUsername(token);
        assertEquals("jane", username);
    }

    @Test
    void validateToken_withCorrectUsername_shouldReturnTrue() {
        String token = jwtUtil.generateToken("john");
        assertTrue(jwtUtil.validateToken(token, "john"));
    }

    @Test
    void validateToken_withWrongUsername_shouldReturnFalse() {
        String token = jwtUtil.generateToken("john");
        assertFalse(jwtUtil.validateToken(token, "jane"));
    }

    @Test
    void validateToken_withInvalidToken_shouldReturnFalse() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_shouldThrowExceptionForMalformedToken() {
        assertThrows(JwtException.class, () -> jwtUtil.extractUsername("bad-token"));
    }
}
