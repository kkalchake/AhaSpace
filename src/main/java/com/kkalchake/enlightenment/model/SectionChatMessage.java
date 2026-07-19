package com.kkalchake.enlightenment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "section_chat_messages")
@Getter
@Setter
@NoArgsConstructor
public class SectionChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "session_id", nullable = false)
    private SectionChatSession session;

    // role is either "user" or "assistant"
    @Column(nullable = false)
    private String role;

    // Stores only the raw user question / raw assistant reply — the section content
    // spliced into the LLM prompt at query time is never persisted here.
    // TEXT type allows content longer than VARCHAR(255) default
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // nullable: only assistant messages have a model name
    @Column
    private String model;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
