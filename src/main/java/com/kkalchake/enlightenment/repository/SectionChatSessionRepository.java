package com.kkalchake.enlightenment.repository;

import com.kkalchake.enlightenment.model.SectionChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SectionChatSessionRepository extends JpaRepository<SectionChatSession, Long> {
    // Spring Data derives SQL from the method name: WHERE user.username = ? AND section.id = ? ORDER BY created_at DESC
    // Scoped by both user and section, unlike ChatSessionRepository which scopes by user only.
    List<SectionChatSession> findByUserUsernameAndSectionIdOrderByCreatedAtDesc(String username, Long sectionId);
}
