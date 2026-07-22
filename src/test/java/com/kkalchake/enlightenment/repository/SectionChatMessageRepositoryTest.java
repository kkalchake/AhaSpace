package com.kkalchake.enlightenment.repository;

import com.kkalchake.enlightenment.model.Course;
import com.kkalchake.enlightenment.model.Section;
import com.kkalchake.enlightenment.model.SectionChatMessage;
import com.kkalchake.enlightenment.model.SectionChatSession;
import com.kkalchake.enlightenment.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class SectionChatMessageRepositoryTest {

    @Autowired
    private SectionChatMessageRepository sectionChatMessageRepository;

    @Autowired
    private SectionChatSessionRepository sectionChatSessionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SectionRepository sectionRepository;

    private SectionChatSession testSession;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setEmail("testuser@example.com");
        user.setPasswordHash("hash");
        userRepository.save(user);

        Course course = new Course();
        course.setTitle("Course A");
        course = courseRepository.save(course);

        Section section = new Section();
        section.setContent("Section content");
        section.setCourse(course);
        section = sectionRepository.save(section);

        testSession = new SectionChatSession();
        testSession.setUser(user);
        testSession.setSection(section);
        testSession.setTitle("Test Session");
        testSession = sectionChatSessionRepository.save(testSession);
    }

    @Test
    void findBySessionIdOrderByCreatedAtAsc_returnsMessagesInOrder() {
        SectionChatMessage m1 = new SectionChatMessage();
        m1.setSession(testSession);
        m1.setRole("user");
        m1.setContent("Hello");
        sectionChatMessageRepository.save(m1);

        SectionChatMessage m2 = new SectionChatMessage();
        m2.setSession(testSession);
        m2.setRole("assistant");
        m2.setContent("Hi there");
        sectionChatMessageRepository.save(m2);

        List<SectionChatMessage> result = sectionChatMessageRepository.findBySessionIdOrderByCreatedAtAsc(testSession.getId());
        assertEquals(2, result.size());
        assertEquals("user", result.get(0).getRole());
    }

    @Test
    void findBySessionIdOrderByCreatedAtAsc_emptySession_returnsEmpty() {
        List<SectionChatMessage> result = sectionChatMessageRepository.findBySessionIdOrderByCreatedAtAsc(testSession.getId());
        assertTrue(result.isEmpty());
    }
}
