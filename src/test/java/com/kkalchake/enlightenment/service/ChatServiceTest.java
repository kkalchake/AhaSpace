package com.kkalchake.enlightenment.service;

import com.kkalchake.enlightenment.dto.*;
import com.kkalchake.enlightenment.model.*;
import com.kkalchake.enlightenment.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private AiProvider aiProvider;
    @Mock private ChatSessionRepository chatSessionRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private UserRepository userRepository;

    private ChatService chatService;
    private User testUser;
    private ChatSession savedSession;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(aiProvider, chatSessionRepository, chatMessageRepository, userRepository);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPasswordHash("hash");

        savedSession = new ChatSession();
        savedSession.setId(1L);
        savedSession.setTitle("Hello AI");
        savedSession.setUser(testUser);
    }

    @Test
    void processMessage_newSession_createsSessionAndPersistsMessages() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatSessionRepository.save(any())).thenReturn(savedSession);
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiProvider.chat("Hello AI")).thenReturn("AI says hi");
        when(aiProvider.getModelName()).thenReturn("gemini-2.5-flash-lite");

        ChatResponse result = chatService.processMessage("Hello AI", "testuser", null);

        assertEquals("AI says hi", result.getResponse());
        assertEquals("gemini-2.5-flash-lite", result.getModel());
        assertEquals(1L, result.getSessionId());
        assertEquals("Hello AI", result.getSessionTitle());
        verify(chatSessionRepository).save(any());
        verify(chatMessageRepository, times(2)).save(any());
    }

    @Test
    void processMessage_newSession_titleTruncatedAt50Chars() {
        String longMessage = "A".repeat(60);
        String expectedTitle = "A".repeat(50) + "...";

        ChatSession sessionWithTruncatedTitle = new ChatSession();
        sessionWithTruncatedTitle.setId(2L);
        sessionWithTruncatedTitle.setTitle(expectedTitle);
        sessionWithTruncatedTitle.setUser(testUser);

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatSessionRepository.save(any())).thenReturn(sessionWithTruncatedTitle);
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiProvider.chat(anyString())).thenReturn("response");
        when(aiProvider.getModelName()).thenReturn("model");

        ChatResponse result = chatService.processMessage(longMessage, "testuser", null);

        assertEquals(expectedTitle, result.getSessionTitle());
    }

    @Test
    void processMessage_existingSession_appendsMessages() {
        when(chatSessionRepository.findById(1L)).thenReturn(Optional.of(savedSession));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiProvider.chat(anyString())).thenReturn("response");
        when(aiProvider.getModelName()).thenReturn("model");

        chatService.processMessage("msg", "testuser", 1L);

        // No new session should be created when sessionId is provided
        verify(chatSessionRepository, never()).save(any());
        verify(chatMessageRepository, times(2)).save(any());
    }

    @Test
    void processMessage_existingSession_wrongOwner_throwsForbidden() {
        User otherUser = new User();
        otherUser.setUsername("otheruser");
        ChatSession otherSession = new ChatSession();
        otherSession.setId(7L);
        otherSession.setUser(otherUser);

        when(chatSessionRepository.findById(7L)).thenReturn(Optional.of(otherSession));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> chatService.processMessage("msg", "testuser", 7L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void processMessage_existingSession_notFound_throwsNotFound() {
        when(chatSessionRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> chatService.processMessage("msg", "testuser", 99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void processMessage_aiProviderThrows_propagatesException() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(chatSessionRepository.save(any())).thenReturn(savedSession);
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiProvider.chat(any())).thenThrow(new RuntimeException("API error"));

        assertThrows(RuntimeException.class,
                () -> chatService.processMessage("Hello", "testuser", null));
    }

    @Test
    void getSessions_returnsMappedDtos() {
        ChatSession s1 = new ChatSession();
        s1.setId(1L);
        s1.setTitle("First");
        s1.setUser(testUser);
        s1.setCreatedAt(LocalDateTime.now());

        ChatSession s2 = new ChatSession();
        s2.setId(2L);
        s2.setTitle("Second");
        s2.setUser(testUser);
        s2.setCreatedAt(LocalDateTime.now());

        when(chatSessionRepository.findByUserUsernameOrderByCreatedAtDesc("testuser"))
                .thenReturn(List.of(s1, s2));

        List<ChatSessionSummaryDto> result = chatService.getSessions("testuser");

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("First", result.get(0).getTitle());
    }

    @Test
    void getSessions_emptyList() {
        when(chatSessionRepository.findByUserUsernameOrderByCreatedAtDesc("testuser"))
                .thenReturn(List.of());

        assertTrue(chatService.getSessions("testuser").isEmpty());
    }

    @Test
    void getSession_success_returnsDetailDto() {
        savedSession.setCreatedAt(LocalDateTime.now());

        ChatMessage m1 = new ChatMessage();
        m1.setId(1L);
        m1.setRole("user");
        m1.setContent("Hello");
        m1.setCreatedAt(LocalDateTime.now());

        ChatMessage m2 = new ChatMessage();
        m2.setId(2L);
        m2.setRole("assistant");
        m2.setContent("Hi");
        m2.setModel("model");
        m2.setCreatedAt(LocalDateTime.now());

        when(chatSessionRepository.findById(1L)).thenReturn(Optional.of(savedSession));
        when(chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(m1, m2));

        ChatSessionDetailDto result = chatService.getSession(1L, "testuser");

        assertEquals(1L, result.getId());
        assertEquals(2, result.getMessages().size());
    }

    @Test
    void getSession_notFound_throwsNotFoundException() {
        when(chatSessionRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> chatService.getSession(99L, "testuser"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getSession_wrongOwner_throwsForbiddenException() {
        User other = new User();
        other.setUsername("otheruser");
        ChatSession otherSession = new ChatSession();
        otherSession.setId(2L);
        otherSession.setUser(other);
        otherSession.setCreatedAt(LocalDateTime.now());

        when(chatSessionRepository.findById(2L)).thenReturn(Optional.of(otherSession));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> chatService.getSession(2L, "testuser"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }
}
