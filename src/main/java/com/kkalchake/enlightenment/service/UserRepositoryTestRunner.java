package com.kkalchake.enlightenment.service;

import com.kkalchake.enlightenment.model.User;
import com.kkalchake.enlightenment.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component // This makes Spring manage this class
public class UserRepositoryTestRunner implements CommandLineRunner {

    private final UserRepository userRepository;

    // Constructor Injection: Spring will provide the UserRepository automatically
    public UserRepositoryTestRunner(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("--- Bootstrapping Data ---");

        // Guard against duplicate on restart: with a persistent DB the user already exists
        if (userRepository.findByUsername("test_engineer").isEmpty()) {
            User testUser = new User();
            testUser.setUsername("test_engineer");
            testUser.setPasswordHash("hashed_password_123");
            userRepository.save(testUser);
        }

        System.out.println("User count in DB: " + userRepository.count());
    }
}