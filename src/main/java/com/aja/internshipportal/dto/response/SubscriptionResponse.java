package com.aja.internshipportal.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.aja.internshipportal.entity.Subscription;
import com.aja.internshipportal.entity.Tier;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class SubscriptionResponse {

	private Long id;
	private String packageName;
	private Tier tier;
    private LocalDate startDate;
    private LocalDate endDate;
    private Subscription.SubscriptionStatus status; // ← still inside Subscription
    private long daysRemaining;
    private LocalDateTime createdAt;
}
