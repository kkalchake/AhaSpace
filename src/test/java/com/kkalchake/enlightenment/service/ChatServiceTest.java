package com.kkalchake.enlightenment.service;

import com.kkalchake.enlightenment.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private AiProvider aiProvider;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(aiProvider);
    }

    @Test
    void processMessage_success() {
        String message = "Hello AI";
        String aiResponse = "Hello! How can I help you?";
        String modelName = "gemini-2.0-flash";

        when(aiProvider.chat(message)).thenReturn(aiResponse);
        when(aiProvider.getModelName()).thenReturn(modelName);

        ChatResponse result = chatService.processMessage(message);

        assertNotNull(result);
        assertEquals(aiResponse, result.getResponse());
        assertEquals(modelName, result.getModel());
        verify(aiProvider).chat(message);
        verify(aiProvider).getModelName();
    }

    @Test
    void processMessage_providerThrowsException() {
        String message = "Hello";
        when(aiProvider.chat(message)).thenThrow(new RuntimeException("API error"));

        assertThrows(RuntimeException.class, () -> chatService.processMessage(message));
        verify(aiProvider).chat(message);
    }

    @Test
    void processMessage_emptyResponse() {
        String message = "Test";
        when(aiProvider.chat(message)).thenReturn("");
        when(aiProvider.getModelName()).thenReturn("gemini-2.0-flash");

        ChatResponse result = chatService.processMessage(message);

        assertEquals("", result.getResponse());
        assertEquals("gemini-2.0-flash", result.getModel());
    }
}
