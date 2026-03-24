package com.aja.internshipportal.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "answers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Answer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private int upvoteCount = 0;

    // Stores user IDs who upvoted — prevents duplicate upvotes
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "answer_upvotes",
        joinColumns = @JoinColumn(name = "answer_id")
    )
    @Column(name = "user_id")
    @Builder.Default
    private Set<Long> upvotedByUserIds = new HashSet<>();

    @Column(nullable = false)
    @Builder.Default
    private boolean accepted = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
