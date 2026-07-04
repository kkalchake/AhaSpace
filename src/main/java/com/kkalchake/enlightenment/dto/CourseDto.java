package com.kkalchake.enlightenment.dto;
import lombok.*;

// Excludes sections/enrolledUsers deliberately: touching those LAZY collections
// outside a transaction would throw LazyInitializationException.
@Data @AllArgsConstructor
public class CourseDto {
    private Long id;
    private String title;
    private String description;
}
