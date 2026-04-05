package com.kkalchake.enlightenment.service;

import com.kkalchake.enlightenment.dto.UserLoginDto;
import com.kkalchake.enlightenment.dto.UserRegistrationDto;
import com.kkalchake.enlightenment.model.User;
import com.kkalchake.enlightenment.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void registerUser(UserRegistrationDto dto) {
        // 1. Business Logic: Check for duplicates
        if (userRepository.findByUsername(dto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        // 2. Map DTO to Entity
        User user = new User();
        user.setUsername(dto.getUsername());

        // 3. Hash Password & Save
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyUser(UserLoginDto dto) {
        Optional<User> userOpt = userRepository.findByUsername(dto.getUsername());

        // Use BCrypt to securely compare raw input with the stored hash
        return userOpt.filter(user -> passwordEncoder.matches(dto.getPassword(), user.getPasswordHash()))
                .isPresent();
    }
}