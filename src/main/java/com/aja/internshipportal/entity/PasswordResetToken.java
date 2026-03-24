package com.aja.internshipportal.entity;



import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // UUID token sent in the reset email link
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    // Expires in 15 minutes
    @Column(nullable = false)
    private LocalDateTime expiryDate;

    // Once used, cannot be reused
    @Column(nullable = false)
    @Builder.Default
    private boolean used = false;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}