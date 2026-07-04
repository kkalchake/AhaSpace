package com.kkalchake.enlightenment.repository;

import com.kkalchake.enlightenment.model.Course;
import com.kkalchake.enlightenment.model.Section;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class SectionRepositoryTest {

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private CourseRepository courseRepository;

    private Course testCourse;

    @BeforeEach
    void setUp() {
        testCourse = new Course();
        testCourse.setTitle("Intro");
        testCourse.setDescription("Intro course");
        testCourse = courseRepository.save(testCourse);
    }

    @Test
    void findByCourseId_returnsSections() {
        Section s1 = new Section();
        s1.setContent("Section 1");
        s1.setCourse(testCourse);
        sectionRepository.save(s1);

        Section s2 = new Section();
        s2.setContent("Section 2");
        s2.setCourse(testCourse);
        sectionRepository.save(s2);

        List<Section> result = sectionRepository.findByCourseId(testCourse.getId());
        assertEquals(2, result.size());
        result.forEach(s -> assertEquals(testCourse.getId(), s.getCourse().getId()));
    }

    @Test
    void findByCourseId_differentCourse_returnsEmpty() {
        Course other = new Course();
        other.setTitle("Other");
        other = courseRepository.save(other);

        Section s = new Section();
        s.setContent("Belongs to other course");
        s.setCourse(other);
        sectionRepository.save(s);

        List<Section> result = sectionRepository.findByCourseId(testCourse.getId());
        assertTrue(result.isEmpty());
    }

    @Test
    void findByCourseId_unknownCourseId_returnsEmpty() {
        assertTrue(sectionRepository.findByCourseId(999L).isEmpty());
    }
}
