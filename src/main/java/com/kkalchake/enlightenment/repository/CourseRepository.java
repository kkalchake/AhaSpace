package com.kkalchake.enlightenment.repository;

import com.kkalchake.enlightenment.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByTitle(String title);

    List<Course> findByIsPublicTrue();

    Optional<Course> findByIdAndIsPublicTrue(Long id);
}
