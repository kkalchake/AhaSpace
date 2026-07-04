package com.kkalchake.enlightenment.repository;

import com.kkalchake.enlightenment.model.Course;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CourseRepositoryTest {

    @Autowired
    private CourseRepository courseRepository;

    @Test
    void save_andFindById_returnsPersistedCourse() {
        Course course = new Course();
        course.setTitle("Intro");
        course.setDescription("Intro course");
        Course saved = courseRepository.save(course);

        assertTrue(courseRepository.findById(saved.getId()).isPresent());
        assertEquals("Intro", courseRepository.findById(saved.getId()).get().getTitle());
    }

    @Test
    void findAll_noCourses_returnsEmpty() {
        List<Course> result = courseRepository.findAll();
        assertTrue(result.isEmpty());
    }

    @Test
    void existsById_unknownId_returnsFalse() {
        assertFalse(courseRepository.existsById(999L));
    }

    @Test
    void existsById_knownId_returnsTrue() {
        Course course = new Course();
        course.setTitle("Intro");
        Course saved = courseRepository.save(course);

        assertTrue(courseRepository.existsById(saved.getId()));
    }
}
