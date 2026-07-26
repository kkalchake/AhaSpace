package com.kkalchake.enlightenment.dto;
import lombok.*;

@Data @AllArgsConstructor
public class PhaseDto {
    private Long id;
    private String title;
    private String description;
    private int orderIndex;
    private Long courseId;
}
