package com.aja.internshipportal.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who performed the action (null = system)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    // e.g. "USER_CREATED", "QUESTION_APPROVED", "PAYMENT_SUCCESS"
    @Column(nullable = false, length = 100)
    private String action;

    // e.g. "User", "Question", "Payment"
    @Column(length = 100)
    private String entityType;

    // ID of the affected record
    private Long entityId;

    // JSON or plain text description of what changed
    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
