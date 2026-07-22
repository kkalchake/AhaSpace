package com.kkalchake.enlightenment.repository;

import com.kkalchake.enlightenment.model.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    // Spring Data derives SQL from the method name: WHERE user.email = ? ORDER BY created_at DESC
    List<ChatSession> findByUserEmailOrderByCreatedAtDesc(String email);
}
