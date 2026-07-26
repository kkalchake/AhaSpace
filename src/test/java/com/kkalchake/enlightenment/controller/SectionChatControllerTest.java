package com.kkalchake.enlightenment.controller;

import com.kkalchake.enlightenment.dto.SectionChatResponse;
import com.kkalchake.enlightenment.dto.SectionChatSessionDetailDto;
import com.kkalchake.enlightenment.dto.SectionChatSessionSummaryDto;
import com.kkalchake.enlightenment.service.SectionChatService;
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
import static org.mockito.ArgumentMatchers.anyLong;
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
class SectionChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SectionChatService sectionChatService;

    // Bring in JwtUtil to generate real tokens
    @Autowired
    private JwtUtil jwtUtil;

    // Helper method to keep tests clean
    private String getAuthHeader() {
        return "Bearer " + jwtUtil.generateToken("testuser");
    }

    @Test
    void chat_requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/courses/1/phases/1/sections/1/chat")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void chat_success() throws Exception {
        SectionChatResponse response = new SectionChatResponse("AI response here", "gemini-2.0-flash", 1L, "Hello AI");
        when(sectionChatService.processMessage(anyLong(), anyString(), anyString(), any())).thenReturn(response);

        mockMvc.perform(post("/api/courses/1/phases/1/sections/1/chat")
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
        mockMvc.perform(post("/api/courses/1/phases/1/sections/1/chat")
                        .header("Authorization", getAuthHeader())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chat_serviceError() throws Exception {
        when(sectionChatService.processMessage(anyLong(), anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("AI service error"));

        mockMvc.perform(post("/api/courses/1/phases/1/sections/1/chat")
                        .header("Authorization", getAuthHeader())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Failed to get AI response"));
    }

    @Test
    void chat_configurationError() throws Exception {
        when(sectionChatService.processMessage(anyLong(), anyString(), anyString(), any()))
                .thenThrow(new IllegalStateException("Not configured"));

        mockMvc.perform(post("/api/courses/1/phases/1/sections/1/chat")
                        .header("Authorization", getAuthHeader())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("AI service not configured"));
    }

    @Test
    void chat_returnsForbiddenWhenSessionNotOwned() throws Exception {
        when(sectionChatService.processMessage(anyLong(), anyString(), anyString(), eq(99L)))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Access denied"));

        mockMvc.perform(post("/api/courses/1/phases/1/sections/1/chat")
                        .header("Authorization", getAuthHeader())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"hi\",\"sessionId\":99}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSessions_returnsListWhenAuthenticated() throws Exception {
        SectionChatSessionSummaryDto dto = new SectionChatSessionSummaryDto(1L, "Test session", java.time.LocalDateTime.now());
        when(sectionChatService.getSessions(1L, "testuser")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/courses/1/phases/1/sections/1/chat/sessions")
                        .header("Authorization", getAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Test session"));
    }

    @Test
    void getSessions_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/courses/1/phases/1/sections/1/chat/sessions"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSession_returnsDetailWhenOwner() throws Exception {
        SectionChatSessionDetailDto dto = new SectionChatSessionDetailDto(1L, "Test", java.time.LocalDateTime.now(), List.of());
        when(sectionChatService.getSession(eq(1L), eq(1L), eq("testuser"))).thenReturn(dto);

        mockMvc.perform(get("/api/courses/1/phases/1/sections/1/chat/sessions/1")
                        .header("Authorization", getAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getSession_returnsForbiddenWhenNotOwner() throws Exception {
        when(sectionChatService.getSession(eq(1L), eq(2L), anyString()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Access denied"));

        mockMvc.perform(get("/api/courses/1/phases/1/sections/1/chat/sessions/2")
                        .header("Authorization", getAuthHeader()))
                .andExpect(status().isForbidden());
    }
}
