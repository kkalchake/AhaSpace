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

    // ResponseStatusException(NOT_FOUND) thrown by the service propagates through
    // Spring MVC and is rendered as 404 by the existing GlobalExceptionHandler.
    @GetMapping("/{courseId}/sections")
    public ResponseEntity<List<SectionDto>> getSections(@PathVariable Long courseId) {
        return ResponseEntity.ok(courseService.getSectionsForCourse(courseId));
    }
}
