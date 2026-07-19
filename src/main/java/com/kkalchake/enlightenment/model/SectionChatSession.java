package com.kkalchake.enlightenment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "section_chat_sessions")
@Getter
@Setter
@NoArgsConstructor
public class SectionChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ManyToOne: each session belongs to exactly one user; produces user_id FK column
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // ManyToOne: each session is scoped to exactly one section, unlike ChatSession which is
    // scoped to a user only. No unique constraint on (user_id, section_id): a user may open
    // multiple separate chat threads against the same section.
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @Column(nullable = false)
    private String title;

    // CreationTimestamp: Hibernate sets this once on INSERT, never updates it
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // mappedBy = "session" means SectionChatMessage.session owns the FK; cascade ensures messages deleted with session
    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<SectionChatMessage> messages = new ArrayList<>();
}
