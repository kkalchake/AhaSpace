package com.kkalchake.enlightenment.controller;

import com.kkalchake.enlightenment.dto.*;
import com.kkalchake.enlightenment.service.SectionChatService;
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
@RequestMapping("/api/courses/{courseId}/sections/{sectionId}/chat")
@RequiredArgsConstructor
public class SectionChatController {

    private final SectionChatService sectionChatService;

    // courseId is kept in the path purely for URL consistency with the rest of the
    // /api/courses/{courseId}/... surface; sections are looked up by their own id, so
    // courseId is never used for lookup or filtering here.
    @PostMapping
    public ResponseEntity<?> chat(@PathVariable Long courseId, @PathVariable Long sectionId,
                                   @Valid @RequestBody SectionChatRequest request, Authentication authentication) {
        log.info("Received section chat message from user: {} for section: {}", authentication.getName(), sectionId);
        try {
            SectionChatResponse response = sectionChatService.processMessage(
                    sectionId, request.getMessage(), authentication.getName(), request.getSessionId());
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
            log.error("Error processing section chat: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Failed to get AI response"));
        }
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<SectionChatSessionSummaryDto>> getSessions(
            @PathVariable Long courseId, @PathVariable Long sectionId, Authentication authentication) {
        return ResponseEntity.ok(sectionChatService.getSessions(sectionId, authentication.getName()));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<SectionChatSessionDetailDto> getSession(
            @PathVariable Long courseId, @PathVariable Long sectionId, @PathVariable Long sessionId,
            Authentication authentication) {
        // ResponseStatusException from the service propagates through Spring MVC with the correct HTTP status
        return ResponseEntity.ok(sectionChatService.getSession(sectionId, sessionId, authentication.getName()));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long courseId, @PathVariable Long sectionId,
                                               @PathVariable Long sessionId, Authentication authentication) {
        sectionChatService.deleteSession(sectionId, sessionId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
