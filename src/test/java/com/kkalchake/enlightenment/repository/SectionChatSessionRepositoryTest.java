package com.kkalchake.enlightenment.repository;

import com.kkalchake.enlightenment.model.Course;
import com.kkalchake.enlightenment.model.Section;
import com.kkalchake.enlightenment.model.SectionChatSession;
import com.kkalchake.enlightenment.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class SectionChatSessionRepositoryTest {

    @Autowired
    private SectionChatSessionRepository sectionChatSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SectionRepository sectionRepository;

    private User testUser;
    private Section testSection;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPasswordHash("hash");
        testUser = userRepository.save(testUser);

        Course course = new Course();
        course.setTitle("Course A");
        course = courseRepository.save(course);

        testSection = new Section();
        testSection.setContent("Section content");
        testSection.setCourse(course);
        testSection = sectionRepository.save(testSection);
    }

    @Test
    void findByUserUsernameAndSectionIdOrderByCreatedAtDesc_returnsSessions() {
        SectionChatSession s1 = new SectionChatSession();
        s1.setUser(testUser);
        s1.setSection(testSection);
        s1.setTitle("First");
        sectionChatSessionRepository.save(s1);

        SectionChatSession s2 = new SectionChatSession();
        s2.setUser(testUser);
        s2.setSection(testSection);
        s2.setTitle("Second");
        sectionChatSessionRepository.save(s2);

        List<SectionChatSession> result = sectionChatSessionRepository
                .findByUserUsernameAndSectionIdOrderByCreatedAtDesc("testuser", testSection.getId());
        assertEquals(2, result.size());
        result.forEach(s -> {
            assertEquals("testuser", s.getUser().getUsername());
            assertEquals(testSection.getId(), s.getSection().getId());
        });
    }

    @Test
    void findByUserUsernameAndSectionIdOrderByCreatedAtDesc_differentUser_returnsEmpty() {
        User other = new User();
        other.setUsername("otheruser");
        other.setPasswordHash("hash");
        userRepository.save(other);

        SectionChatSession s = new SectionChatSession();
        s.setUser(other);
        s.setSection(testSection);
        s.setTitle("Other's session");
        sectionChatSessionRepository.save(s);

        List<SectionChatSession> result = sectionChatSessionRepository
                .findByUserUsernameAndSectionIdOrderByCreatedAtDesc("testuser", testSection.getId());
        assertTrue(result.isEmpty());
    }

    @Test
    void findByUserUsernameAndSectionIdOrderByCreatedAtDesc_differentSection_returnsEmpty() {
        // Same user, but a session tied to a second section must not show up when querying the first
        Section otherSection = new Section();
        otherSection.setContent("Other section content");
        otherSection.setCourse(testSection.getCourse());
        otherSection = sectionRepository.save(otherSection);

        SectionChatSession s = new SectionChatSession();
        s.setUser(testUser);
        s.setSection(otherSection);
        s.setTitle("Session in other section");
        sectionChatSessionRepository.save(s);

        List<SectionChatSession> result = sectionChatSessionRepository
                .findByUserUsernameAndSectionIdOrderByCreatedAtDesc("testuser", testSection.getId());
        assertTrue(result.isEmpty());
    }

    @Test
    void findById_existingSession_returnsPresent() {
        SectionChatSession s = new SectionChatSession();
        s.setUser(testUser);
        s.setSection(testSection);
        s.setTitle("Test");
        SectionChatSession saved = sectionChatSessionRepository.save(s);

        assertTrue(sectionChatSessionRepository.findById(saved.getId()).isPresent());
    }

    @Test
    void findById_unknownId_returnsEmpty() {
        assertTrue(sectionChatSessionRepository.findById(999L).isEmpty());
    }
}
