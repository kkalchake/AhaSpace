package com.kkalchake.enlightenment.controller;

import com.kkalchake.enlightenment.dto.CourseDto;
import com.kkalchake.enlightenment.dto.PhaseDto;
import com.kkalchake.enlightenment.dto.SectionDto;
import com.kkalchake.enlightenment.service.CourseService;
import com.kkalchake.enlightenment.util.JwtUtil;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-must-be-32-chars-long",
        "gemini.api.key=test-key"
})
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseService courseService;

    @Autowired
    private JwtUtil jwtUtil;

    private String getAuthHeader() {
        return "Bearer " + jwtUtil.generateToken("testuser");
    }

    @Test
    void getCourses_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/courses"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCourses_returnsListWhenAuthenticated() throws Exception {
        CourseDto dto = new CourseDto(1L, "Intro", "Intro course", true,
                "Source Name", "https://source.example/repo", "MIT License",
                List.of("Insight one", "Insight two"));
        when(courseService.getAllCourses()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/courses")
                        .header("Authorization", getAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Intro"))
                .andExpect(jsonPath("$[0].description").value("Intro course"))
                // Wire key must be exactly "isPublic" - Lombok's isPublic()
                // getter would otherwise serialize as "public" without the
                // @JsonProperty annotation on CourseDto.
                .andExpect(jsonPath("$[0].isPublic").value(true))
                .andExpect(jsonPath("$[0].public").doesNotExist())
                .andExpect(jsonPath("$[0].sourceName").value("Source Name"))
                .andExpect(jsonPath("$[0].sourceUrl").value("https://source.example/repo"))
                .andExpect(jsonPath("$[0].sourceLicense").value("MIT License"))
                .andExpect(jsonPath("$[0].insights[0]").value("Insight one"))
                .andExpect(jsonPath("$[0].insights[1]").value("Insight two"));
    }

    @Test
    void getCourses_emptyList() throws Exception {
        when(courseService.getAllCourses()).thenReturn(List.of());

        mockMvc.perform(get("/api/courses")
                        .header("Authorization", getAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void getPhases_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/courses/1/phases"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPhases_returnsListWhenAuthenticated() throws Exception {
        PhaseDto dto = new PhaseDto(1L, "Phase 0: Setup & Tooling", "Phase description", 0, 1L);
        when(courseService.getPhasesForCourse(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/courses/1/phases")
                        .header("Authorization", getAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Phase 0: Setup & Tooling"))
                .andExpect(jsonPath("$[0].orderIndex").value(0))
                .andExpect(jsonPath("$[0].courseId").value(1));
    }

    @Test
    void getPhases_courseNotFound_returns404() throws Exception {
        when(courseService.getPhasesForCourse(99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));

        mockMvc.perform(get("/api/courses/99/phases")
                        .header("Authorization", getAuthHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Course not found"));
    }

    @Test
    void getSections_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/courses/1/phases/1/sections"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSections_returnsListWhenAuthenticated() throws Exception {
        SectionDto dto = new SectionDto(1L, "Section Title", 1, "Section content", 1L);
        when(courseService.getSectionsForPhase(1L, 1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/courses/1/phases/1/sections")
                        .header("Authorization", getAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Section Title"))
                .andExpect(jsonPath("$[0].orderIndex").value(1))
                .andExpect(jsonPath("$[0].content").value("Section content"))
                .andExpect(jsonPath("$[0].phaseId").value(1));
    }

    @Test
    void getSections_phaseNotFound_returns404() throws Exception {
        when(courseService.getSectionsForPhase(1L, 99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Phase not found"));

        mockMvc.perform(get("/api/courses/1/phases/99/sections")
                        .header("Authorization", getAuthHeader()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Phase not found"));
    }
}
