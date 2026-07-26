package com.kkalchake.enlightenment.controller;

import com.kkalchake.enlightenment.dto.*;
import com.kkalchake.enlightenment.service.CourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<List<CourseDto>> getCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/{courseId}/phases")
    public ResponseEntity<List<PhaseDto>> getPhases(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getPhasesForCourse(courseId));
    }

    @GetMapping("/{courseId}/phases/{phaseId}/sections")
    public ResponseEntity<List<SectionDto>> getSections(@PathVariable Long courseId, @PathVariable Long phaseId) {
        return ResponseEntity.ok(courseService.getSectionsForPhase(courseId, phaseId));
    }
}
