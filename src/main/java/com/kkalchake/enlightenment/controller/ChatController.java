package com.kkalchake.enlightenment.controller;

import com.kkalchake.enlightenment.dto.ChatRequest;
import com.kkalchake.enlightenment.dto.ChatResponse;
import com.kkalchake.enlightenment.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<?> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat message");
        try {
            ChatResponse response = chatService.processMessage(request.getMessage());
            return ResponseEntity.ok(response);
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
}
