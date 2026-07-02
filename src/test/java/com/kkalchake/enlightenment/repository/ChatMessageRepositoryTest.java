package com.kkalchake.enlightenment.repository;

import com.kkalchake.enlightenment.model.ChatMessage;
import com.kkalchake.enlightenment.model.ChatSession;
import com.kkalchake.enlightenment.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ChatMessageRepositoryTest {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private UserRepository userRepository;

    private ChatSession testSession;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setUsername("testuser");
        user.setPasswordHash("hash");
        userRepository.save(user);

        testSession = new ChatSession();
        testSession.setUser(user);
        testSession.setTitle("Test Session");
        testSession = chatSessionRepository.save(testSession);
    }

    @Test
    void findBySessionIdOrderByCreatedAtAsc_returnsMessagesInOrder() {
        ChatMessage m1 = new ChatMessage();
        m1.setSession(testSession);
        m1.setRole("user");
        m1.setContent("Hello");
        chatMessageRepository.save(m1);

        ChatMessage m2 = new ChatMessage();
        m2.setSession(testSession);
        m2.setRole("assistant");
        m2.setContent("Hi there");
        chatMessageRepository.save(m2);

        List<ChatMessage> result = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(testSession.getId());
        assertEquals(2, result.size());
        assertEquals("user", result.get(0).getRole());
    }

    @Test
    void findBySessionIdOrderByCreatedAtAsc_emptySession_returnsEmpty() {
        List<ChatMessage> result = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(testSession.getId());
        assertTrue(result.isEmpty());
    }
}
