package com.kkalchake.enlightenment.controller;

import com.kkalchake.enlightenment.dto.CourseDto;
import com.kkalchake.enlightenment.dto.PhaseDto;
import com.kkalchake.enlightenment.dto.SectionDto;
import com.kkalchake.enlightenment.service.CourseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-must-be-32-chars-long",
        "gemini.api.key=test-key"
})
class PublicCourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService courseService;

    // No Authorization header on any request in this class - these routes
    // must work for anonymous callers, and a stale/invalid token must not be
    // required or cause a failure (SecurityConfig permits GET /api/public/**
    // outright; JwtFilter never rejects a request either way).

    @Test
    void getPublicCourses_noAuthHeader_returnsOk() throws Exception {
        CourseDto dto = new CourseDto(1L, "Intro", "Intro course", true,
                "Source Name", "https://source.example/repo", "MIT License",
                List.of("Insight one"));
        when(courseService.getPublicCourses()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/public/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].isPublic").value(true));
    }

    @Test
    void getPublicCourse_noAuthHeader_returnsOk() throws Exception {
        CourseDto dto = new CourseDto(1L, "Intro", "Intro course", true,
                "Source Name", "https://source.example/repo", "MIT License", List.of());
        when(courseService.getPublicCourse(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/public/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.insights").isArray())
                .andExpect(jsonPath("$.insights").isEmpty());
    }

    @Test
    void getPublicCourse_notPublicOrMissing_returns404NotForbidden() throws Exception {
        when(courseService.getPublicCourse(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        mockMvc.perform(get("/api/public/courses/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Course not found"));
    }

    @Test
    void getPublicPhases_noAuthHeader_returnsOk() throws Exception {
        PhaseDto dto = new PhaseDto(1L, "Phase 0: Setup & Tooling", "Phase description", 0, 1L);
        when(courseService.getPublicPhasesForCourse(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/public/courses/1/phases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Phase 0: Setup & Tooling"));
    }

    @Test
    void getPublicPhases_courseNotPublic_returns404() throws Exception {
        when(courseService.getPublicPhasesForCourse(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        mockMvc.perform(get("/api/public/courses/99/phases"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPublicSections_noAuthHeader_returnsOk() throws Exception {
        SectionDto dto = new SectionDto(1L, "Section Title", 1, "Section content", 1L);
        when(courseService.getPublicSectionsForPhase(1L, 1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/public/courses/1/phases/1/sections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Section Title"));
    }

    @Test
    void getPublicSections_courseNotPublic_returns404() throws Exception {
        when(courseService.getPublicSectionsForPhase(99L, 1L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        mockMvc.perform(get("/api/public/courses/99/phases/1/sections"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postToPublicCourses_nonGetMethod_isForbidden() throws Exception {
        // Only GET is permitted under /api/public/**; SecurityConfig's matcher
        // is method-scoped, so POST falls through to anyRequest().authenticated()
        // and is rejected before it ever reaches a controller (no POST mapping
        // exists on this path anyway).
        mockMvc.perform(post("/api/public/courses"))
                .andExpect(status().isForbidden());
    }
}
