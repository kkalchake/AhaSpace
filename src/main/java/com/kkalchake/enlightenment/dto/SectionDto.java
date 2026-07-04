package com.kkalchake.enlightenment.dto;
import lombok.*;

@Data @AllArgsConstructor
public class SectionDto {
    private Long id;
    private String content;
    private Long courseId;
}
