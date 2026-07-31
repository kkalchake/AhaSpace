package com.kkalchake.enlightenment.service;

import com.kkalchake.enlightenment.dto.*;
import com.kkalchake.enlightenment.model.Course;
import com.kkalchake.enlightenment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final PhaseRepository phaseRepository;
    private final SectionRepository sectionRepository;

    @Transactional(readOnly = true)
    public List<CourseDto> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // Public-demo counterpart to getAllCourses: same mapping, filtered to
    // courses flagged for public display.
    @Transactional(readOnly = true)
    public List<CourseDto> getPublicCourses() {
        return courseRepository.findByIsPublicTrue().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CourseDto getPublicCourse(Long courseId) {
        Course course = courseRepository.findByIdAndIsPublicTrue(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        return toDto(course);
    }

    // A course id that exists but isn't public must 404 exactly like an id
    // that doesn't exist at all - never 403 - so this guard and the
    // authenticated getPhasesForCourse's existsById guard deliberately return
    // the same status/message for their respective "not visible to caller"
    // cases.
    @Transactional(readOnly = true)
    public List<PhaseDto> getPublicPhasesForCourse(Long courseId) {
        courseRepository.findByIdAndIsPublicTrue(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        return getPhasesForCourse(courseId);
    }

    @Transactional(readOnly = true)
    public List<SectionDto> getPublicSectionsForPhase(Long courseId, Long phaseId) {
        courseRepository.findByIdAndIsPublicTrue(courseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found"));
        return getSectionsForPhase(courseId, phaseId);
    }

    @Transactional(readOnly = true)
    public List<PhaseDto> getPhasesForCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        return phaseRepository.findByCourseIdOrderByOrderIndexAsc(courseId).stream()
                .map(p -> new PhaseDto(p.getId(), p.getTitle(), p.getDescription(), p.getOrderIndex(), courseId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SectionDto> getSectionsForPhase(Long courseId, Long phaseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        var phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phase not found"));
        if (!phase.getCourse().getId().equals(courseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Phase not found");
        }
        return sectionRepository.findByPhaseIdOrderByOrderIndexAsc(phaseId).stream()
                .map(s -> new SectionDto(s.getId(), s.getTitle(), s.getOrderIndex(), s.getContent(), phaseId))
                .collect(Collectors.toList());
    }

    // Single mapping point for Course -> CourseDto, used by every
    // course-returning method (authenticated and public alike) so the two
    // paths can't drift out of sync on what a course looks like on the wire.
    private CourseDto toDto(Course c) {
        return new CourseDto(
                c.getId(),
                c.getTitle(),
                c.getDescription(),
                c.isPublic(),
                c.getSourceName(),
                c.getSourceUrl(),
                c.getSourceLicense(),
                splitInsights(c.getInsights())
        );
    }

    // \R matches any line terminator (\n, \r\n, or \r), so this handles
    // insights text regardless of which line-ending convention wrote it.
    // Blank lines are dropped rather than kept as empty-string entries.
    private static List<String> splitInsights(String insights) {
        if (insights == null || insights.isBlank()) {
            return List.of();
        }
        return Arrays.stream(insights.split("\\R"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
