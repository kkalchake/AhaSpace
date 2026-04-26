package com.kkalchake.enlightenment.service;

import com.kkalchake.enlightenment.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    @Qualifier("geminiProvider")
    private final AiProvider aiProvider;

    public ChatResponse processMessage(String message) {
        log.info("Processing chat message");
        String aiResponse = aiProvider.chat(message);
        return new ChatResponse(aiResponse, aiProvider.getModelName());
    }
}
