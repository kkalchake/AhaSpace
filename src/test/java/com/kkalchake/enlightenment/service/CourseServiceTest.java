package com.kkalchake.enlightenment.service;

import com.kkalchake.enlightenment.dto.CourseDto;
import com.kkalchake.enlightenment.dto.PhaseDto;
import com.kkalchake.enlightenment.dto.SectionDto;
import com.kkalchake.enlightenment.model.Course;
import com.kkalchake.enlightenment.model.Phase;
import com.kkalchake.enlightenment.model.Section;
import com.kkalchake.enlightenment.repository.CourseRepository;
import com.kkalchake.enlightenment.repository.PhaseRepository;
import com.kkalchake.enlightenment.repository.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private PhaseRepository phaseRepository;
    @Mock private SectionRepository sectionRepository;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, phaseRepository, sectionRepository);
    }

    @Test
    void getAllCourses_returnsMappedDtos() {
        Course c1 = new Course();
        c1.setId(1L);
        c1.setTitle("Intro");
        c1.setDescription("Intro course");

        Course c2 = new Course();
        c2.setId(2L);
        c2.setTitle("Advanced");
        c2.setDescription("Advanced course");

        when(courseRepository.findAll()).thenReturn(List.of(c1, c2));

        List<CourseDto> result = courseService.getAllCourses();

        assertEquals(2, result.size());
        assertEquals("Intro", result.get(0).getTitle());
        assertEquals("Advanced", result.get(1).getTitle());
    }

    @Test
    void getAllCourses_emptyList() {
        when(courseRepository.findAll()).thenReturn(List.of());

        assertTrue(courseService.getAllCourses().isEmpty());
    }

    @Test
    void getPhasesForCourse_courseNotFound_throwsNotFound() {
        when(courseRepository.existsById(99L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> courseService.getPhasesForCourse(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getPhasesForCourse_courseExists_returnsMappedDtos() {
        Phase p1 = new Phase();
        p1.setId(1L);
        p1.setTitle("Phase 0: Setup & Tooling");
        p1.setOrderIndex(0);
        p1.setDescription("Phase description");

        when(courseRepository.existsById(1L)).thenReturn(true);
        when(phaseRepository.findByCourseIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(p1));

        List<PhaseDto> result = courseService.getPhasesForCourse(1L);

        assertEquals(1, result.size());
        assertEquals("Phase 0: Setup & Tooling", result.get(0).getTitle());
        assertEquals(0, result.get(0).getOrderIndex());
        assertEquals(1L, result.get(0).getCourseId());
    }

    @Test
    void getPhasesForCourse_courseExistsNoPhases_returnsEmpty() {
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(phaseRepository.findByCourseIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());

        assertTrue(courseService.getPhasesForCourse(1L).isEmpty());
    }

    @Test
    void getSectionsForPhase_courseNotFound_throwsNotFound() {
        when(courseRepository.existsById(99L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> courseService.getSectionsForPhase(99L, 1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getSectionsForPhase_phaseNotFound_throwsNotFound() {
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(phaseRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> courseService.getSectionsForPhase(1L, 99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getSectionsForPhase_phaseBelongsToDifferentCourse_throwsNotFound() {
        Course otherCourse = new Course();
        otherCourse.setId(2L);

        Phase phase = new Phase();
        phase.setId(1L);
        phase.setCourse(otherCourse);

        when(courseRepository.existsById(1L)).thenReturn(true);
        when(phaseRepository.findById(1L)).thenReturn(Optional.of(phase));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> courseService.getSectionsForPhase(1L, 1L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getSectionsForPhase_phaseExists_returnsMappedDtos() {
        Course course = new Course();
        course.setId(1L);

        Phase phase = new Phase();
        phase.setId(1L);
        phase.setCourse(course);

        Section s1 = new Section();
        s1.setId(1L);
        s1.setTitle("Section 1");
        s1.setOrderIndex(1);
        s1.setContent("Section 1");

        when(courseRepository.existsById(1L)).thenReturn(true);
        when(phaseRepository.findById(1L)).thenReturn(Optional.of(phase));
        when(sectionRepository.findByPhaseIdOrderByOrderIndexAsc(1L)).thenReturn(List.of(s1));

        List<SectionDto> result = courseService.getSectionsForPhase(1L, 1L);

        assertEquals(1, result.size());
        assertEquals("Section 1", result.get(0).getTitle());
        assertEquals(1, result.get(0).getOrderIndex());
        assertEquals("Section 1", result.get(0).getContent());
        assertEquals(1L, result.get(0).getPhaseId());
    }

    @Test
    void getSectionsForPhase_phaseExistsNoSections_returnsEmpty() {
        Course course = new Course();
        course.setId(1L);

        Phase phase = new Phase();
        phase.setId(1L);
        phase.setCourse(course);

        when(courseRepository.existsById(1L)).thenReturn(true);
        when(phaseRepository.findById(1L)).thenReturn(Optional.of(phase));
        when(sectionRepository.findByPhaseIdOrderByOrderIndexAsc(1L)).thenReturn(List.of());

        assertTrue(courseService.getSectionsForPhase(1L, 1L).isEmpty());
    }
}
