package com.kkalchake.enlightenment.repository;

import com.kkalchake.enlightenment.model.Course;
import com.kkalchake.enlightenment.model.Phase;
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
    private PhaseRepository phaseRepository;

    @Autowired
    private CourseRepository courseRepository;

    private Phase testPhase;

    @BeforeEach
    void setUp() {
        Course course = new Course();
        course.setTitle("Intro Curriculum");
        course = courseRepository.save(course);

        testPhase = new Phase();
        testPhase.setTitle("Phase 0");
        testPhase.setOrderIndex(0);
        testPhase.setCourse(course);
        testPhase = phaseRepository.save(testPhase);
    }

    @Test
    void findByPhaseId_returnsSections() {
        Section s1 = new Section();
        s1.setTitle("Section 1");
        s1.setOrderIndex(1);
        s1.setContent("Section 1");
        s1.setPhase(testPhase);
        sectionRepository.save(s1);

        Section s2 = new Section();
        s2.setTitle("Section 2");
        s2.setOrderIndex(2);
        s2.setContent("Section 2");
        s2.setPhase(testPhase);
        sectionRepository.save(s2);

        List<Section> result = sectionRepository.findByPhaseIdOrderByOrderIndexAsc(testPhase.getId());
        assertEquals(2, result.size());
        result.forEach(s -> assertEquals(testPhase.getId(), s.getPhase().getId()));
    }

    @Test
    void findByPhaseId_differentPhase_returnsEmpty() {
        Course course = new Course();
        course.setTitle("Other Curriculum");
        course = courseRepository.save(course);

        Phase other = new Phase();
        other.setTitle("Other Phase");
        other.setOrderIndex(0);
        other.setCourse(course);
        other = phaseRepository.save(other);

        Section s = new Section();
        s.setTitle("Belongs to other phase");
        s.setOrderIndex(1);
        s.setContent("Belongs to other phase");
        s.setPhase(other);
        sectionRepository.save(s);

        List<Section> result = sectionRepository.findByPhaseIdOrderByOrderIndexAsc(testPhase.getId());
        assertTrue(result.isEmpty());
    }

    @Test
    void findByPhaseId_unknownPhaseId_returnsEmpty() {
        assertTrue(sectionRepository.findByPhaseIdOrderByOrderIndexAsc(999L).isEmpty());
    }

    @Test
    void findByPhaseId_outOfInsertionOrder_returnsSortedByOrderIndex() {
        // Insert in reverse order-index order: id assignment (insertion order)
        // must not leak into the returned ordering - only orderIndex should.
        Section higherIndexFirst = new Section();
        higherIndexFirst.setTitle("Second");
        higherIndexFirst.setOrderIndex(2);
        higherIndexFirst.setContent("Second content");
        higherIndexFirst.setPhase(testPhase);
        sectionRepository.save(higherIndexFirst);

        Section lowerIndexSecond = new Section();
        lowerIndexSecond.setTitle("First");
        lowerIndexSecond.setOrderIndex(1);
        lowerIndexSecond.setContent("First content");
        lowerIndexSecond.setPhase(testPhase);
        sectionRepository.save(lowerIndexSecond);

        List<Section> result = sectionRepository.findByPhaseIdOrderByOrderIndexAsc(testPhase.getId());
        assertEquals(2, result.size());
        assertEquals("First", result.get(0).getTitle());
        assertEquals("Second", result.get(1).getTitle());
    }
}
