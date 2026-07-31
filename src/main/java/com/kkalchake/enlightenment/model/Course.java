package com.kkalchake.enlightenment.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "courses")
@Getter
@Setter
@NoArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Field initializer (not just relying on the DB column default) matters
    // because CurriculumSeedRunner/CurriculumPhaseSeeder build Course objects
    // in memory and save them through JPA before any DB default applies -
    // without this, a newly constructed Course would read as `false` in Java
    // anyway (the primitive's own zero-value), but making it explicit here
    // keeps the "private by default" invariant visible at the model level
    // rather than left implicit in Java's primitive defaulting.
    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    // Nullable: only courses sourced from an external attributed curriculum
    // (currently just the pilot) populate these. decisions.md calls for new
    // nullable columns on `courses`, not a side table, so attribution lives
    // directly on the entity rather than behind an @ElementCollection.
    private String sourceName;
    private String sourceUrl;
    private String sourceLicense;

    // Newline-separated bullet lines, one insight per line. Splitting into a
    // real array happens in CourseService.toDto (server-side), so the wire
    // format the frontend consumes is always List<String>, never a blob it
    // has to parse itself.
    @Column(columnDefinition = "TEXT")
    private String insights;

    @ManyToMany(mappedBy = "enrolledCourses", fetch = FetchType.LAZY)
    private Set<User> enrolledUsers = new HashSet<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Phase> phases = new ArrayList<>();
}
