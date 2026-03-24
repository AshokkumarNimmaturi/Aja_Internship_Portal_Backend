package com.aja.internshipportal.entity;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private CoursePackage aPackage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tier tier;

    // Razorpay order ID returned when order is created
    @Column(nullable = false, unique = true, length = 100)
    private String razorpayOrderId;

    // Razorpay payment ID returned after user pays
    @Column(unique = true, length = 100)
    private String razorpayPaymentId;

    // Razorpay signature — used to verify payment authenticity
    @Column(unique = true, length = 512)
    private String razorpaySignature;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // CREATED → SUCCESS or FAILED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.CREATED;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public enum PaymentStatus {
        CREATED, SUCCESS, FAILED
    }
}
