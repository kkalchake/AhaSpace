package com.kkalchake.enlightenment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SectionChatResponse {
    private String response;
    private String model;
    private Long sessionId;
    private String sessionTitle;
}
