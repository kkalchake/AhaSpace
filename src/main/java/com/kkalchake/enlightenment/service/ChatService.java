package com.kkalchake.enlightenment.service;

import com.kkalchake.enlightenment.dto.*;
import com.kkalchake.enlightenment.model.*;
import com.kkalchake.enlightenment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    // @Qualifier on the field is copied to the constructor parameter by Lombok, resolving the correct AiProvider bean
    @Qualifier("geminiProvider")
    private final AiProvider aiProvider;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatResponse processMessage(String message, String email, Long sessionId) {
        log.info("Processing chat message for user: {}", email);
        ChatSession session;

        if (sessionId == null) {
            var user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User not found"));
            session = new ChatSession();
            session.setUser(user);
            session.setTitle(buildTitle(message));
            session = chatSessionRepository.save(session);
        } else {
            session = chatSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
            // Ownership check: prevent users from writing into another user's session
            if (!session.getUser().getEmail().equals(email)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
        }

        ChatMessage userMsg = new ChatMessage();
        userMsg.setSession(session);
        userMsg.setRole("user");
        userMsg.setContent(message);
        chatMessageRepository.save(userMsg);

        String aiResponse = aiProvider.chat(message);
        String modelName = aiProvider.getModelName();

        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setSession(session);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(aiResponse);
        assistantMsg.setModel(modelName);
        chatMessageRepository.save(assistantMsg);

        return new ChatResponse(aiResponse, modelName, session.getId(), session.getTitle());
    }

    @Transactional(readOnly = true)
    public List<ChatSessionSummaryDto> getSessions(String email) {
        return chatSessionRepository.findByUserEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(s -> new ChatSessionSummaryDto(s.getId(), s.getTitle(), s.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChatSessionDetailDto getSession(Long sessionId, String email) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        if (!session.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        List<ChatMessageDto> messageDtos = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(m -> new ChatMessageDto(m.getId(), m.getRole(), m.getContent(), m.getModel(), m.getCreatedAt()))
                .collect(Collectors.toList());
        return new ChatSessionDetailDto(session.getId(), session.getTitle(), session.getCreatedAt(), messageDtos);
    }

    // Deletes the session row and, via cascade configured on ChatSession.messages,
    // all of its ChatMessage rows. delete(session) is used (rather than deleteById)
    // because the entity is already loaded for the ownership check below — no extra
    // query needed, and cascade behavior is identical either way.
    @Transactional
    public void deleteSession(Long sessionId, String email) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        // Ownership check: prevent users from deleting another user's session
        if (!session.getUser().getEmail().equals(email)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        chatSessionRepository.delete(session);
    }

    // Truncate to 50 chars so the sidebar title stays readable
    private String buildTitle(String message) {
        return message.length() > 50 ? message.substring(0, 50) + "..." : message;
    }
}
