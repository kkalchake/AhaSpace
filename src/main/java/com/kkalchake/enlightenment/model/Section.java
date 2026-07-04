package com.kkalchake.enlightenment.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sections")
@Getter
@Setter
@NoArgsConstructor
public class Section {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TEXT column: section content can exceed the default VARCHAR(255) length
    @Column(columnDefinition = "TEXT")
    private String content;

    // Owning side: sections table holds course_id FK
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;
}
