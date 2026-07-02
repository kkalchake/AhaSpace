package com.kkalchake.enlightenment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    @NotBlank(message = "Message is required")
    private String message;

    // No validation annotation — null means the caller wants a new session created
    private Long sessionId;
}
