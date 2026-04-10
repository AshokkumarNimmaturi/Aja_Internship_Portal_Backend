package com.aja.internshipportal.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "support_calls")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportCall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ NEW: Added callSid for precise tracking
    @Column(unique = true)
    private String callSid;

    @Column(nullable = false)
    private String callerNumber;

    private String agentNumber;
    private String agentName;

    @Enumerated(EnumType.STRING)
    private CallStatus status;

    private Integer duration; // in seconds

    @CreationTimestamp
    private LocalDateTime timestamp;

    public enum CallStatus {
        COMPLETED,  // Call was answered and finished
        ANSWERED,   // Call is currently in progress
        MISSED,     // Agent didn't answer (Hunt failed)
        ABANDONED,  // Caller hung up while in queue
        BUSY        // All agents were busy/offline
    }
}
