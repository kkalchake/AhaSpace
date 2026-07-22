package com.kkalchake.enlightenment.service;

import com.kkalchake.enlightenment.dto.*;
import com.kkalchake.enlightenment.model.*;
import com.kkalchake.enlightenment.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectionChatServiceTest {

    @Mock private AiProvider aiProvider;
    @Mock private SectionChatSessionRepository sectionChatSessionRepository;
    @Mock private SectionChatMessageRepository sectionChatMessageRepository;
    @Mock private UserRepository userRepository;
    @Mock private SectionRepository sectionRepository;

    private SectionChatService sectionChatService;
    private User testUser;
    private Section testSection;
    private SectionChatSession savedSession;

    @BeforeEach
    void setUp() {
        sectionChatService = new SectionChatService(
                aiProvider, sectionChatSessionRepository, sectionChatMessageRepository, userRepository, sectionRepository);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("testuser");
        testUser.setPasswordHash("hash");

        testSection = new Section();
        testSection.setId(1L);
        testSection.setContent("Section body text");

        savedSession = new SectionChatSession();
        savedSession.setId(1L);
        savedSession.setTitle("Hello AI");
        savedSession.setUser(testUser);
        savedSession.setSection(testSection);
    }

    @Test
    void processMessage_newSession_createsSessionAndPersistsMessages() {
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(testSection));
        when(userRepository.findByEmail("testuser")).thenReturn(Optional.of(testUser));
        when(sectionChatSessionRepository.save(any())).thenReturn(savedSession);
        when(sectionChatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiProvider.chat(anyString())).thenReturn("AI says hi");
        when(aiProvider.getModelName()).thenReturn("gemini-2.5-flash-lite");

        SectionChatResponse result = sectionChatService.processMessage(1L, "Hello AI", "testuser", null);

        assertEquals("AI says hi", result.getResponse());
        assertEquals("gemini-2.5-flash-lite", result.getModel());
        assertEquals(1L, result.getSessionId());
        assertEquals("Hello AI", result.getSessionTitle());
        verify(sectionChatSessionRepository).save(any());
        verify(sectionChatMessageRepository, times(2)).save(any());
    }

    @Test
    void processMessage_newSession_titleTruncatedAt50Chars() {
        String longMessage = "A".repeat(60);
        String expectedTitle = "A".repeat(50) + "...";

        SectionChatSession sessionWithTruncatedTitle = new SectionChatSession();
        sessionWithTruncatedTitle.setId(2L);
        sessionWithTruncatedTitle.setTitle(expectedTitle);
        sessionWithTruncatedTitle.setUser(testUser);
        sessionWithTruncatedTitle.setSection(testSection);

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(testSection));
        when(userRepository.findByEmail("testuser")).thenReturn(Optional.of(testUser));
        when(sectionChatSessionRepository.save(any())).thenReturn(sessionWithTruncatedTitle);
        when(sectionChatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiProvider.chat(anyString())).thenReturn("response");
        when(aiProvider.getModelName()).thenReturn("model");

        SectionChatResponse result = sectionChatService.processMessage(1L, longMessage, "testuser", null);

        assertEquals(expectedTitle, result.getSessionTitle());
    }

    @Test
    void processMessage_existingSession_appendsMessages() {
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(testSection));
        when(sectionChatSessionRepository.findById(1L)).thenReturn(Optional.of(savedSession));
        when(sectionChatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiProvider.chat(anyString())).thenReturn("response");
        when(aiProvider.getModelName()).thenReturn("model");

        sectionChatService.processMessage(1L, "msg", "testuser", 1L);

        // No new session should be created when sessionId is provided
        verify(sectionChatSessionRepository, never()).save(any());
        verify(sectionChatMessageRepository, times(2)).save(any());
    }

    @Test
    void processMessage_existingSession_wrongOwner_throwsForbidden() {
        User otherUser = new User();
        otherUser.setEmail("otheruser");
        SectionChatSession otherSession = new SectionChatSession();
        otherSession.setId(7L);
        otherSession.setUser(otherUser);
        otherSession.setSection(testSection);

        when(sectionRepository.findById(1L)).thenReturn(Optional.of(testSection));
        when(sectionChatSessionRepository.findById(7L)).thenReturn(Optional.of(otherSession));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> sectionChatService.processMessage(1L, "msg", "testuser", 7L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void processMessage_existingSession_notFound_throwsNotFound() {
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(testSection));
        when(sectionChatSessionRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> sectionChatService.processMessage(1L, "msg", "testuser", 99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void processMessage_sectionNotFound_throwsNotFound() {
        when(sectionRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> sectionChatService.processMessage(99L, "msg", "testuser", null));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(userRepository, sectionChatSessionRepository, sectionChatMessageRepository, aiProvider);
    }

    @Test
    void processMessage_sessionBelongsToOtherSection_throwsNotFound() {
        Section otherSection = new Section();
        otherSection.setId(2L);
        otherSection.setContent("Other content");

        SectionChatSession sessionForOtherSection = new SectionChatSession();
        sessionForOtherSection.setId(5L);
        sessionForOtherSection.setUser(testUser);
        sessionForOtherSection.setSection(otherSection);

        // URL says sectionId=1, but the session actually belongs to section 2
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(testSection));
        when(sectionChatSessionRepository.findById(5L)).thenReturn(Optional.of(sessionForOtherSection));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> sectionChatService.processMessage(1L, "msg", "testuser", 5L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void processMessage_aiProviderThrows_propagatesException() {
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(testSection));
        when(userRepository.findByEmail("testuser")).thenReturn(Optional.of(testUser));
        when(sectionChatSessionRepository.save(any())).thenReturn(savedSession);
        when(sectionChatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiProvider.chat(any())).thenThrow(new RuntimeException("API error"));

        assertThrows(RuntimeException.class,
                () -> sectionChatService.processMessage(1L, "Hello", "testuser", null));
    }

    @Test
    void processMessage_storesRawMessageAndSendsAugmentedPromptToAiProvider() {
        when(sectionRepository.findById(1L)).thenReturn(Optional.of(testSection));
        when(userRepository.findByEmail("testuser")).thenReturn(Optional.of(testUser));
        when(sectionChatSessionRepository.save(any())).thenReturn(savedSession);
        when(sectionChatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiProvider.chat(anyString())).thenReturn("response");
        when(aiProvider.getModelName()).thenReturn("model");

        sectionChatService.processMessage(1L, "What is this about?", "testuser", null);

        ArgumentCaptor<SectionChatMessage> messageCaptor = ArgumentCaptor.forClass(SectionChatMessage.class);
        verify(sectionChatMessageRepository, times(2)).save(messageCaptor.capture());
        SectionChatMessage storedUserMessage = messageCaptor.getAllValues().get(0);
        assertEquals("What is this about?", storedUserMessage.getContent());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiProvider).chat(promptCaptor.capture());
        assertEquals("Section content:\nSection body text\n\nQuestion: What is this about?", promptCaptor.getValue());
    }

    @Test
    void getSessions_returnsMappedDtos() {
        SectionChatSession s1 = new SectionChatSession();
        s1.setId(1L);
        s1.setTitle("First");
        s1.setUser(testUser);
        s1.setSection(testSection);
        s1.setCreatedAt(LocalDateTime.now());

        SectionChatSession s2 = new SectionChatSession();
        s2.setId(2L);
        s2.setTitle("Second");
        s2.setUser(testUser);
        s2.setSection(testSection);
        s2.setCreatedAt(LocalDateTime.now());

        when(sectionChatSessionRepository.findByUserEmailAndSectionIdOrderByCreatedAtDesc("testuser", 1L))
                .thenReturn(List.of(s1, s2));

        List<SectionChatSessionSummaryDto> result = sectionChatService.getSessions(1L, "testuser");

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals("First", result.get(0).getTitle());
    }

    @Test
    void getSessions_emptyList() {
        when(sectionChatSessionRepository.findByUserEmailAndSectionIdOrderByCreatedAtDesc("testuser", 1L))
                .thenReturn(List.of());

        assertTrue(sectionChatService.getSessions(1L, "testuser").isEmpty());
    }

    @Test
    void getSession_success_returnsDetailDto() {
        savedSession.setCreatedAt(LocalDateTime.now());

        SectionChatMessage m1 = new SectionChatMessage();
        m1.setId(1L);
        m1.setRole("user");
        m1.setContent("Hello");
        m1.setCreatedAt(LocalDateTime.now());

        SectionChatMessage m2 = new SectionChatMessage();
        m2.setId(2L);
        m2.setRole("assistant");
        m2.setContent("Hi");
        m2.setModel("model");
        m2.setCreatedAt(LocalDateTime.now());

        when(sectionChatSessionRepository.findById(1L)).thenReturn(Optional.of(savedSession));
        when(sectionChatMessageRepository.findBySessionIdOrderByCreatedAtAsc(1L)).thenReturn(List.of(m1, m2));

        SectionChatSessionDetailDto result = sectionChatService.getSession(1L, 1L, "testuser");

        assertEquals(1L, result.getId());
        assertEquals(2, result.getMessages().size());
    }

    @Test
    void getSession_notFound_throwsNotFoundException() {
        when(sectionChatSessionRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> sectionChatService.getSession(1L, 99L, "testuser"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getSession_wrongOwner_throwsForbiddenException() {
        User other = new User();
        other.setEmail("otheruser");
        SectionChatSession otherSession = new SectionChatSession();
        otherSession.setId(2L);
        otherSession.setUser(other);
        otherSession.setSection(testSection);
        otherSession.setCreatedAt(LocalDateTime.now());

        when(sectionChatSessionRepository.findById(2L)).thenReturn(Optional.of(otherSession));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> sectionChatService.getSession(1L, 2L, "testuser"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void getSession_differentSection_throwsNotFoundException() {
        Section otherSection = new Section();
        otherSection.setId(2L);

        SectionChatSession sessionForOtherSection = new SectionChatSession();
        sessionForOtherSection.setId(3L);
        sessionForOtherSection.setUser(testUser);
        sessionForOtherSection.setSection(otherSection);
        sessionForOtherSection.setCreatedAt(LocalDateTime.now());

        when(sectionChatSessionRepository.findById(3L)).thenReturn(Optional.of(sessionForOtherSection));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> sectionChatService.getSession(1L, 3L, "testuser"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
