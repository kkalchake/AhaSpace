package com.kkalchake.enlightenment.controller;

import com.kkalchake.enlightenment.dto.ChatResponse;
import com.kkalchake.enlightenment.dto.ChatSessionDetailDto;
import com.kkalchake.enlightenment.dto.ChatSessionSummaryDto;
import com.kkalchake.enlightenment.service.ChatService;
import com.kkalchake.enlightenment.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-must-be-32-chars-long",
        "gemini.api.key=test-key"
})
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    // Bring in JwtUtil to generate real tokens
    @Autowired
    private JwtUtil jwtUtil;

    // Helper method to keep tests clean
    private String getAuthHeader() {
        return "Bearer " + jwtUtil.generateToken("testuser");
    }

    @Test
    void chat_requiresAuthentication() throws Exception {
        // No token provided, expecting a block
        mockMvc.perform(post("/api/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void chat_success() throws Exception {
        ChatResponse response = new ChatResponse("AI response here", "gemini-2.0-flash", 1L, "Hello AI");
        when(chatService.processMessage(anyString(), anyString(), any())).thenReturn(response);

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", getAuthHeader())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello AI\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("AI response here"))
                .andExpect(jsonPath("$.model").value("gemini-2.0-flash"))
                .andExpect(jsonPath("$.sessionId").value(1))
                .andExpect(jsonPath("$.sessionTitle").value("Hello AI"));
    }

    @Test
    void chat_validationError_blankMessage() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .header("Authorization", getAuthHeader())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_serviceError() throws Exception {
        when(chatService.processMessage(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("AI service error"));

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", getAuthHeader())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Failed to get AI response"));
    }

    @Test
    void chat_configurationError() throws Exception {
        when(chatService.processMessage(anyString(), anyString(), any()))
                .thenThrow(new IllegalStateException("Not configured"));

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", getAuthHeader())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AI service not configured"));
    }

    @Test
    void getSessions_returnsListWhenAuthenticated() throws Exception {
        ChatSessionSummaryDto dto = new ChatSessionSummaryDto(1L, "Test session", java.time.LocalDateTime.now());
        when(chatService.getSessions("testuser")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/chat/sessions")
                        .header("Authorization", getAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test session"));
    }

    @Test
    void getSessions_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/chat/sessions"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSession_returnsDetailWhenOwner() throws Exception {
        ChatSessionDetailDto dto = new ChatSessionDetailDto(1L, "Test", java.time.LocalDateTime.now(), List.of());
        when(chatService.getSession(eq(1L), eq("testuser"))).thenReturn(dto);

        mockMvc.perform(get("/api/chat/sessions/1")
                        .header("Authorization", getAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getSession_returnsForbiddenWhenNotOwner() throws Exception {
        when(chatService.getSession(eq(2L), anyString()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Access denied"));

        mockMvc.perform(get("/api/chat/sessions/2")
                        .header("Authorization", getAuthHeader()))
                .andExpect(status().isForbidden());
    }

    @Test
    void chat_returnsForbiddenWhenSessionNotOwned() throws Exception {
        when(chatService.processMessage(anyString(), anyString(), eq(99L)))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Access denied"));

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", getAuthHeader())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\",\"sessionId\":99}"))
                .andExpect(status().isForbidden());
    }
}
