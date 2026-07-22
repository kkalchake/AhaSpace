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
    void extractEmail_shouldReturnCorrectEmail() {
        String token = jwtUtil.generateToken("jane");
        String email = jwtUtil.extractEmail(token);
        assertEquals("jane", email);
    }

    @Test
    void validateToken_withCorrectEmail_shouldReturnTrue() {
        String token = jwtUtil.generateToken("john");
        assertTrue(jwtUtil.validateToken(token, "john"));
    }

    @Test
    void validateToken_withWrongEmail_shouldReturnFalse() {
        String token = jwtUtil.generateToken("john");
        assertFalse(jwtUtil.validateToken(token, "jane"));
    }

    @Test
    void validateToken_withInvalidToken_shouldReturnFalse() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_shouldThrowExceptionForMalformedToken() {
        assertThrows(JwtException.class, () -> jwtUtil.extractEmail("bad-token"));
    }
}
