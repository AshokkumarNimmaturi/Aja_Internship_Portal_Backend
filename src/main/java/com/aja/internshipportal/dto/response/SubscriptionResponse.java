package com.aja.internshipportal.dto.response;

import com.aja.internshipportal.entity.Subscription;
import com.aja.internshipportal.entity.Tier;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter @Setter @Builder
public class SubscriptionResponse {

    private Long id;

    // package name e.g. "Backend Package"
    private String packageName;

    // Tier from separate Tier.java entity
    private Tier tier;

    private LocalDate startDate;
    private LocalDate endDate;

    // ACTIVE, EXPIRED, CANCELLED
    private Subscription.SubscriptionStatus status;

    // calculated in service — how many days left
    private long daysRemaining;

    private LocalDateTime createdAt;
}