package com.kkalchake.enlightenment.dto;
import lombok.*;
import java.time.LocalDateTime;

@Data @AllArgsConstructor
public class SectionChatSessionSummaryDto {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
}
