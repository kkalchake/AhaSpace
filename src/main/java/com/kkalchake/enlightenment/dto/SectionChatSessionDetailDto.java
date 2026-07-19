package com.kkalchake.enlightenment.dto;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @AllArgsConstructor
public class SectionChatSessionDetailDto {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
    private List<SectionChatMessageDto> messages;
}
