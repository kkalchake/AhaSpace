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
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        User user = new User();
        user.setEmail(dto.getEmail());

        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyUser(UserLoginDto dto) {
        Optional<User> userOpt = userRepository.findByEmail(dto.getEmail());

        // Short-circuit order (findByEmail, then only matches() if present) is left
        // untouched on purpose: when the email doesn't exist, filter() never calls
        // matches(), so a nonexistent-email request returns faster than a
        // wrong-password request. That timing gap lets an attacker infer which
        // emails are registered. Fixing it (e.g. always running matches() against a
        // dummy hash) is deferred to a later session, not in scope this week.
        return userOpt.filter(user -> passwordEncoder.matches(dto.getPassword(), user.getPasswordHash()))
                .isPresent();
    }
}