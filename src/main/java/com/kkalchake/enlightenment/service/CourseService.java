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
    private final SectionRepository sectionRepository;

    @Transactional(readOnly = true)
    public List<CourseDto> getAllCourses() {
        // Mapping to DTO happens inside the transaction: readOnly = true keeps the
        // session open long enough for this stream, even though Course fields used here aren't LAZY.
        return courseRepository.findAll().stream()
                .map(c -> new CourseDto(c.getId(), c.getTitle(), c.getDescription()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SectionDto> getSectionsForCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found");
        }
        return sectionRepository.findByCourseId(courseId).stream()
                .map(s -> new SectionDto(s.getId(), s.getContent(), courseId))
                .collect(Collectors.toList());
    }
}
