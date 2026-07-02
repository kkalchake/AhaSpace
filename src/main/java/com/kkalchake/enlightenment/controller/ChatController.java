package com.kkalchake.enlightenment.controller;

import com.kkalchake.enlightenment.dto.*;
import com.kkalchake.enlightenment.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<?> chat(@Valid @RequestBody ChatRequest request, Authentication authentication) {
        log.info("Received chat message from user: {}", authentication.getName());
        try {
            ChatResponse response = chatService.processMessage(
                    request.getMessage(), authentication.getName(), request.getSessionId());
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            // Must be caught BEFORE RuntimeException — ResponseStatusException extends RuntimeException,
            // so without this clause it would be swallowed as a generic 400
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("error", e.getReason() != null ? e.getReason() : "Request error"));
        } catch (IllegalStateException e) {
            log.error("Configuration error: {}", e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "AI service not configured"));
        } catch (RuntimeException e) {
            log.error("Error processing chat: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get AI response"));
        }
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<ChatSessionSummaryDto>> getSessions(Authentication authentication) {
        return ResponseEntity.ok(chatService.getSessions(authentication.getName()));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ChatSessionDetailDto> getSession(
            @PathVariable Long sessionId,
            Authentication authentication) {
        // ResponseStatusException from the service propagates through Spring MVC with the correct HTTP status
        return ResponseEntity.ok(chatService.getSession(sessionId, authentication.getName()));
    }
}
