package com.kkalchake.enlightenment.repository;

import com.kkalchake.enlightenment.model.ChatSession;
import com.kkalchake.enlightenment.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ChatSessionRepositoryTest {

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setEmail("testuser@example.com");
        testUser.setPasswordHash("hash");
        testUser = userRepository.save(testUser);
    }

    @Test
    void findByUserEmailOrderByCreatedAtDesc_returnsSessions() {
        ChatSession s1 = new ChatSession();
        s1.setUser(testUser);
        s1.setTitle("First");
        chatSessionRepository.save(s1);

        ChatSession s2 = new ChatSession();
        s2.setUser(testUser);
        s2.setTitle("Second");
        chatSessionRepository.save(s2);

        List<ChatSession> result = chatSessionRepository.findByUserEmailOrderByCreatedAtDesc("testuser@example.com");
        assertEquals(2, result.size());
        result.forEach(s -> assertEquals("testuser@example.com", s.getUser().getEmail()));
    }

    @Test
    void findByUserEmailOrderByCreatedAtDesc_differentUser_returnsEmpty() {
        User other = new User();
        other.setEmail("otheruser@example.com");
        other.setPasswordHash("hash");
        userRepository.save(other);

        ChatSession s = new ChatSession();
        s.setUser(other);
        s.setTitle("Other's session");
        chatSessionRepository.save(s);

        List<ChatSession> result = chatSessionRepository.findByUserEmailOrderByCreatedAtDesc("testuser@example.com");
        assertTrue(result.isEmpty());
    }

    @Test
    void findById_existingSession_returnsPresent() {
        ChatSession s = new ChatSession();
        s.setUser(testUser);
        s.setTitle("Test");
        ChatSession saved = chatSessionRepository.save(s);

        assertTrue(chatSessionRepository.findById(saved.getId()).isPresent());
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        assertTrue(chatSessionRepository.findById(999L).isEmpty());
    }
}
