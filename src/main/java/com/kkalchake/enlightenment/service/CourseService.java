package com.kkalchake.enlightenment.service;

import com.kkalchake.enlightenment.dto.*;
import com.kkalchake.enlightenment.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
                .map(c -> new CourseDto(c.getId(), c.getTitle(), c.getDescription()))
                .collect(Collectors.toList());
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
}
