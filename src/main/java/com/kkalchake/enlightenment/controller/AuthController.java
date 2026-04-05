package com.kkalchake.enlightenment.controller;

import com.kkalchake.enlightenment.dto.UserLoginDto;
import com.kkalchake.enlightenment.dto.UserRegistrationDto;
import com.kkalchake.enlightenment.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody UserRegistrationDto dto) {
        userService.registerUser(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@Valid @RequestBody UserLoginDto dto) {
        if (userService.verifyUser(dto)) {
            return ResponseEntity.ok(Map.of("message", "Login successful"));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid username or password"));
    }
}