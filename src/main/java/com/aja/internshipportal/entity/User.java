package com.aja.internshipportal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

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

    // ✅ REPLACED: Professional Support Status (Enum)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SupportStatus status = SupportStatus.OFFLINE;

    // ✅ NEW: Real-time Call Status
    @Column(nullable = false)
    @Builder.Default
    private boolean inCall = false;

    @Column(length = 500)
    private String profilePicture;

    // ✅ ADDED: Technology interests for profiling
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_interests", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "interest")
    private List<String> interests;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum Role {
        ADMIN, TUTOR, EMPLOYEE, SUBSCRIBER
    }

    // ✅ NEW: Support Status Options
    public enum SupportStatus {
        AVAILABLE,  // Online and ready to receive calls
        BREAK,      // Online but temporarily not receiving calls
        OFFLINE     // Signed off
    }
}
