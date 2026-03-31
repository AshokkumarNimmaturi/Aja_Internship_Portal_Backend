package com.aja.internshipportal.service.impl;

import com.aja.internshipportal.dto.response.SubscriptionResponse;
import com.aja.internshipportal.entity.CoursePackage;
import com.aja.internshipportal.entity.Payment;
import com.aja.internshipportal.entity.Subscription;
import com.aja.internshipportal.entity.Tier;
import com.aja.internshipportal.entity.User;
import com.aja.internshipportal.exception.AppException;
import com.aja.internshipportal.repository.PackageRepository;
import com.aja.internshipportal.repository.SubscriptionRepository;
import com.aja.internshipportal.repository.UserRepository;
import com.aja.internshipportal.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PackageRepository packageRepository;

    // ── Get my subscriptions ──
    @Override
    @Transactional
    public List<SubscriptionResponse> getMySubscriptions(String email) {
        User user = getUserByEmail(email);

        return subscriptionRepository.findByUser(user)
                .stream()
                .map(this::mapToSubscriptionResponse)
                .collect(Collectors.toList());
    }

    // ── Create subscription after payment verified ──
    // called internally by PaymentService — not exposed as API directly
    @Override
    @Transactional
    public void createSubscription(Payment payment,
                                   CoursePackage coursePackage,
                                   Tier tier) {

        LocalDate startDate = LocalDate.now();

        // calculate end date based on tier
        LocalDate endDate = switch (tier) {
            case BASIC   -> startDate.plusDays(30);
            case STANDARD -> startDate.plusDays(90);
            case PREMIUM  -> startDate.plusDays(180);
            case BUNDLE   -> startDate.plusDays(180);
        };

        Subscription subscription = Subscription.builder()
                .user(payment.getUser())
                .aPackage(coursePackage)
                .tier(tier)
                .startDate(startDate)
                .endDate(endDate)
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .payment(payment)
                .build();

        subscriptionRepository.save(subscription);
    }

    // ── Check active subscription ──
    // returns true if user has valid unexpired subscription
    @Override
    public boolean hasActiveSubscription(String email, Long packageId) {

        User user = getUserByEmail(email);

        CoursePackage coursePackage = packageRepository.findById(packageId)
                .orElseThrow(() ->
                    AppException.notFound("Package not found")
                );

        // check specific package subscription
        boolean hasSpecific = subscriptionRepository
                .findActiveSubscription(user, coursePackage, LocalDate.now())
                .isPresent();

        if (hasSpecific) return true;

        // check if user has bundle subscription
        // bundle gives access to all packages
        return subscriptionRepository
                .findAllActiveSubscriptions(user, LocalDate.now())
                .stream()
                .anyMatch(sub ->
                    sub.getAPackage().getPackageType() ==
                    CoursePackage.PackageType.BUNDLE
                );
    }

    // ── helper ──

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                    AppException.notFound("User not found")
                );
    }

    private SubscriptionResponse mapToSubscriptionResponse(
            Subscription subscription) {

        // calculate days remaining
        long daysRemaining = ChronoUnit.DAYS.between(
                LocalDate.now(),
                subscription.getEndDate()
        );

        // if expired set to 0
        daysRemaining = Math.max(daysRemaining, 0);

        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .packageName(subscription.getAPackage().getName())
                .tier(subscription.getTier())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .status(subscription.getStatus())
                .daysRemaining(daysRemaining)
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}