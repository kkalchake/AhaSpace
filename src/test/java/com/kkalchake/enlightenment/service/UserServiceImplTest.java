package com.kkalchake.enlightenment.service;

import com.kkalchake.enlightenment.dto.UserLoginDto;
import com.kkalchake.enlightenment.dto.UserRegistrationDto;
import com.kkalchake.enlightenment.model.User;
import com.kkalchake.enlightenment.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void registerUser_success() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail("newuser@example.com");
        dto.setPassword("password123");

        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");

        userService.registerUser(dto);

        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_duplicateEmail_throwsException() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setEmail("existing@example.com");
        dto.setPassword("password123");

        when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(new User()));

        assertThrows(IllegalArgumentException.class, () -> userService.registerUser(dto));
        verify(userRepository, never()).save(any());
    }

    @Test
    void verifyUser_success() {
        UserLoginDto dto = new UserLoginDto();
        dto.setEmail("john@example.com");
        dto.setPassword("secret");

        User user = new User();
        user.setEmail("john@example.com");
        user.setPasswordHash("hashedSecret");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hashedSecret")).thenReturn(true);

        assertTrue(userService.verifyUser(dto));
    }

    @Test
    void verifyUser_wrongPassword() {
        UserLoginDto dto = new UserLoginDto();
        dto.setEmail("john@example.com");
        dto.setPassword("wrong");

        User user = new User();
        user.setPasswordHash("hashedSecret");

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashedSecret")).thenReturn(false);

        assertFalse(userService.verifyUser(dto));
    }

    @Test
    void verifyUser_userNotFound() {
        UserLoginDto dto = new UserLoginDto();
        dto.setEmail("unknown@example.com");
        dto.setPassword("password");

        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertFalse(userService.verifyUser(dto));
    }
}
