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

    private String description;

    // mappedBy = "enrolledCourses": User.enrolledCourses owns the FK columns via user_courses
    @ManyToMany(mappedBy = "enrolledCourses", fetch = FetchType.LAZY)
    private Set<User> enrolledUsers = new HashSet<>();

    // mappedBy = "course": Section.course owns the FK; cascade+orphanRemoval deletes sections with the course
    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Section> sections = new ArrayList<>();
}
