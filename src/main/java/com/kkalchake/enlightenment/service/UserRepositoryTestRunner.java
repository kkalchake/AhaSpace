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

        // 1. Create a User using Lombok or manual constructor
        User testUser = new User();
        testUser.setUsername("test_engineer");
        testUser.setPasswordHash("hashed_password_123");

        // 2. Use the Repository to save it
        userRepository.save(testUser);

        System.out.println("User saved! Count in DB: " + userRepository.count());
    }
}