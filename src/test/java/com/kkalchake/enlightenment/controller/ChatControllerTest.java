package com.kkalchake.enlightenment.controller;

import com.kkalchake.enlightenment.dto.ChatResponse;
import com.kkalchake.enlightenment.service.ChatService;
import com.kkalchake.enlightenment.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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

    @MockitoBean
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
        ChatResponse response = new ChatResponse("AI response here", "gemini-2.0-flash");
        when(chatService.processMessage(anyString())).thenReturn(response);

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", getAuthHeader())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello AI\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("AI response here"))
                .andExpect(jsonPath("$.model").value("gemini-2.0-flash"));
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
        when(chatService.processMessage(anyString()))
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
        when(chatService.processMessage(anyString()))
                .thenThrow(new IllegalStateException("Not configured"));

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", getAuthHeader())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AI service not configured"));
    }
}