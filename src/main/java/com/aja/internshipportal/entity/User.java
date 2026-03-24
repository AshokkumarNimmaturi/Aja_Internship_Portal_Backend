package com.aja.internshipportal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(unique = true, length = 20)
    private String phone;

    // ADMIN, TUTOR, EMPLOYEE, SUBSCRIBER
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    // false = account is deactivated (soft delete)
    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    // true on all internal accounts — forces password change on first login
    @Column(nullable = false)
    @Builder.Default
    private boolean firstLogin = false;

    @Column(length = 500)
    private String profilePicture;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Role {
        ADMIN, TUTOR, EMPLOYEE, SUBSCRIBER
    }
}
