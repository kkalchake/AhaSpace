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
public class SectionChatService {

    // @Qualifier on the field is copied to the constructor parameter by Lombok, resolving the correct AiProvider bean
    @Qualifier("geminiProvider")
    private final AiProvider aiProvider;
    private final SectionChatSessionRepository sectionChatSessionRepository;
    private final SectionChatMessageRepository sectionChatMessageRepository;
    private final UserRepository userRepository;
    private final SectionRepository sectionRepository;

    @Transactional
    public SectionChatResponse processMessage(Long sectionId, String message, String username, Long sessionId) {
        log.info("Processing section chat message for user: {}, section: {}", username, sectionId);

        Section section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Section not found"));

        SectionChatSession session;

        if (sessionId == null) {
            var user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "User not found"));
            session = new SectionChatSession();
            session.setUser(user);
            session.setSection(section);
            session.setTitle(buildTitle(message));
            session = sectionChatSessionRepository.save(session);
        } else {
            session = sectionChatSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
            // Ownership check: prevent users from writing into another user's session
            if (!session.getUser().getUsername().equals(username)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
            }
            // Section-match check: a sessionId that exists but belongs to a different section
            // (e.g. reused across two section chat URLs) must 404, not silently redirect the chat
            if (!session.getSection().getId().equals(sectionId)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
            }
        }

        // Persist the raw question only — the augmented prompt sent to the AI is built fresh
        // below and never stored, so re-reading history doesn't re-splice section content in.
        SectionChatMessage userMsg = new SectionChatMessage();
        userMsg.setSession(session);
        userMsg.setRole("user");
        userMsg.setContent(message);
        sectionChatMessageRepository.save(userMsg);

        String prompt = "Section content:\n" + section.getContent() + "\n\nQuestion: " + message;
        String aiResponse = aiProvider.chat(prompt);
        String modelName = aiProvider.getModelName();

        SectionChatMessage assistantMsg = new SectionChatMessage();
        assistantMsg.setSession(session);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(aiResponse);
        assistantMsg.setModel(modelName);
        sectionChatMessageRepository.save(assistantMsg);

        return new SectionChatResponse(aiResponse, modelName, session.getId(), session.getTitle());
    }

    @Transactional(readOnly = true)
    public List<SectionChatSessionSummaryDto> getSessions(Long sectionId, String username) {
        return sectionChatSessionRepository.findByUserUsernameAndSectionIdOrderByCreatedAtDesc(username, sectionId)
                .stream()
                .map(s -> new SectionChatSessionSummaryDto(s.getId(), s.getTitle(), s.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SectionChatSessionDetailDto getSession(Long sectionId, Long sessionId, String username) {
        SectionChatSession session = sectionChatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        if (!session.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        if (!session.getSection().getId().equals(sectionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
        List<SectionChatMessageDto> messageDtos = sectionChatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(m -> new SectionChatMessageDto(m.getId(), m.getRole(), m.getContent(), m.getModel(), m.getCreatedAt()))
                .collect(Collectors.toList());
        return new SectionChatSessionDetailDto(session.getId(), session.getTitle(), session.getCreatedAt(), messageDtos);
    }

    // Deletes the session row and, via cascade configured on SectionChatSession.messages,
    // all of its SectionChatMessage rows. delete(session) is used (rather than deleteById)
    // because the entity is already loaded for the ownership/section checks below.
    @Transactional
    public void deleteSession(Long sectionId, Long sessionId, String username) {
        SectionChatSession session = sectionChatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));
        // Ownership check: prevent users from deleting another user's session
        if (!session.getUser().getUsername().equals(username)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        // Section-match check: a sessionId that exists but belongs to a different section
        // must 404, not delete a session outside the section the caller is scoped to
        if (!session.getSection().getId().equals(sectionId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
        }
        sectionChatSessionRepository.delete(session);
    }

    // Truncate to 50 chars so the sidebar title stays readable
    private String buildTitle(String message) {
        return message.length() > 50 ? message.substring(0, 50) + "..." : message;
    }
}
