package com.kkalchake.enlightenment.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    private final PasswordEncoder encoder = new SecurityConfig(null, null).passwordEncoder();

    @Test
    void newHashesUseArgon2Prefix() {
        assertTrue(encoder.encode("password123").startsWith("{argon2}"));
    }

    @Test
    void unprefixedLegacyBcryptHashStillMatches() {
        String legacyHash = new BCryptPasswordEncoder().encode("password123");
        assertTrue(encoder.matches("password123", legacyHash));
    }
}
