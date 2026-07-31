package com.kkalchake.enlightenment.controller;

import com.kkalchake.enlightenment.dto.*;
import com.kkalchake.enlightenment.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Kept as its own controller rather than new methods on CourseController: this
// way the entire "no authentication required" surface lives under one
// @RequestMapping prefix, visible in a single file, and can't be widened by
// accident by someone adding a route next to an authenticated one. Security
// enforces the "GET only, no auth" contract at the SecurityConfig layer
// (permitAll scoped to HttpMethod.GET on /api/public/**) - this controller
// only ever defines GET mappings, so the two stay in agreement.
@Slf4j
@RestController
@RequestMapping("/api/public/courses")
@RequiredArgsConstructor
public class PublicCourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<List<CourseDto>> getPublicCourses() {
        return ResponseEntity.ok(courseService.getPublicCourses());
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDto> getPublicCourse(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getPublicCourse(courseId));
    }

    @GetMapping("/{courseId}/phases")
    public ResponseEntity<List<PhaseDto>> getPublicPhases(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getPublicPhasesForCourse(courseId));
    }

    @GetMapping("/{courseId}/phases/{phaseId}/sections")
    public ResponseEntity<List<SectionDto>> getPublicSections(@PathVariable Long courseId,
                                                                @PathVariable Long phaseId) {
        return ResponseEntity.ok(courseService.getPublicSectionsForPhase(courseId, phaseId));
    }
}
