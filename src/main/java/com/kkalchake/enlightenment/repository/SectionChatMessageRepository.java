package com.kkalchake.enlightenment.repository;

import com.kkalchake.enlightenment.model.SectionChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SectionChatMessageRepository extends JpaRepository<SectionChatMessage, Long> {
    List<SectionChatMessage> findBySessionIdOrderByCreatedAtAsc(Long sessionId);
}
