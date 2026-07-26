package com.kkalchake.enlightenment.repository;

import com.kkalchake.enlightenment.model.Course;
import com.kkalchake.enlightenment.model.Phase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class PhaseRepositoryTest {

    @Autowired
    private PhaseRepository phaseRepository;

    @Autowired
    private CourseRepository courseRepository;

    private Course testCourse;

    @BeforeEach
    void setUp() {
        testCourse = new Course();
        testCourse.setTitle("AI Engineering From Scratch");
        testCourse = courseRepository.save(testCourse);
    }

    @Test
    void existsByTitle_unknownTitle_returnsFalse() {
        assertFalse(phaseRepository.existsByTitle("Phase 0: Setup & Tooling"));
    }

    @Test
    void existsByTitle_knownTitle_returnsTrue() {
        Phase phase = new Phase();
        phase.setTitle("Phase 0: Setup & Tooling");
        phase.setOrderIndex(0);
        phase.setCourse(testCourse);
        phaseRepository.save(phase);

        assertTrue(phaseRepository.existsByTitle("Phase 0: Setup & Tooling"));
    }

    @Test
    void findByCourseId_returnsPhasesSortedByOrderIndex() {
        Phase p2 = new Phase();
        p2.setTitle("Phase 1: Math Foundations");
        p2.setOrderIndex(1);
        p2.setCourse(testCourse);
        phaseRepository.save(p2);

        Phase p1 = new Phase();
        p1.setTitle("Phase 0: Setup & Tooling");
        p1.setOrderIndex(0);
        p1.setCourse(testCourse);
        phaseRepository.save(p1);

        List<Phase> result = phaseRepository.findByCourseIdOrderByOrderIndexAsc(testCourse.getId());
        assertEquals(2, result.size());
        assertEquals("Phase 0: Setup & Tooling", result.get(0).getTitle());
        assertEquals("Phase 1: Math Foundations", result.get(1).getTitle());
    }

    @Test
    void findByCourseId_differentCourse_returnsEmpty() {
        Course other = new Course();
        other.setTitle("Other Curriculum");
        other = courseRepository.save(other);

        Phase phase = new Phase();
        phase.setTitle("Belongs to other course");
        phase.setOrderIndex(0);
        phase.setCourse(other);
        phaseRepository.save(phase);

        assertTrue(phaseRepository.findByCourseIdOrderByOrderIndexAsc(testCourse.getId()).isEmpty());
    }

    @Test
    void findByCourseId_unknownCourseId_returnsEmpty() {
        assertTrue(phaseRepository.findByCourseIdOrderByOrderIndexAsc(999L).isEmpty());
    }
}
