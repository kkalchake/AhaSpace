package com.kkalchake.enlightenment.service;

import com.kkalchake.enlightenment.dto.CourseDto;
import com.kkalchake.enlightenment.dto.SectionDto;
import com.kkalchake.enlightenment.model.Course;
import com.kkalchake.enlightenment.model.Section;
import com.kkalchake.enlightenment.repository.CourseRepository;
import com.kkalchake.enlightenment.repository.SectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private SectionRepository sectionRepository;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(courseRepository, sectionRepository);
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
    void getSectionsForCourse_courseNotFound_throwsNotFound() {
        when(courseRepository.existsById(99L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> courseService.getSectionsForCourse(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getSectionsForCourse_courseExists_returnsMappedDtos() {
        Section s1 = new Section();
        s1.setId(1L);
        s1.setContent("Section 1");

        when(courseRepository.existsById(1L)).thenReturn(true);
        when(sectionRepository.findByCourseId(1L)).thenReturn(List.of(s1));

        List<SectionDto> result = courseService.getSectionsForCourse(1L);

        assertEquals(1, result.size());
        assertEquals("Section 1", result.get(0).getContent());
        assertEquals(1L, result.get(0).getCourseId());
    }

    @Test
    void getSectionsForCourse_courseExistsNoSections_returnsEmpty() {
        when(courseRepository.existsById(1L)).thenReturn(true);
        when(sectionRepository.findByCourseId(1L)).thenReturn(List.of());

        assertTrue(courseService.getSectionsForCourse(1L).isEmpty());
    }
}
