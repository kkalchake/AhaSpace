package com.kkalchake.enlightenment.dto;
import lombok.*;
import java.time.LocalDateTime;

@Data @AllArgsConstructor
public class SectionChatMessageDto {
    private Long id;
    private String role;
    private String content;
    private String model;
    private LocalDateTime createdAt;
}
