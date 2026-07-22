package com.kkalchake.enlightenment.model;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name ="users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // NOT NULL from day one, not added as a nullable column then backfilled:
    // the dev DB has no rows yet (ddl-auto: update creates the table fresh),
    // so there's no existing data that would violate the constraint on migration.
    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    // Owning side of the M2M: user_courses join table holds both FKs.
    // Set (not List) avoids duplicate enrollments and MultipleBagFetchException
    // if a second LAZY collection is ever added to this entity.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_courses",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "course_id"))
    private Set<Course> enrolledCourses = new HashSet<>();
}