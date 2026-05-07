//// PATH: src/main/java/com/aja/internshipportal/service/impl/SubscriptionServiceImpl.java
//
//package com.aja.internshipportal.service.impl;
//
//import com.aja.internshipportal.dto.response.SubscriptionResponse;
//import com.aja.internshipportal.entity.CoursePackage;
//import com.aja.internshipportal.entity.Payment;
//import com.aja.internshipportal.entity.Subscription;
//import com.aja.internshipportal.entity.Tier;
//import com.aja.internshipportal.entity.User;
//import com.aja.internshipportal.exception.AppException;
//import com.aja.internshipportal.repository.PackageRepository;
//import com.aja.internshipportal.repository.SubscriptionRepository;
//import com.aja.internshipportal.repository.UserRepository;
//import com.aja.internshipportal.service.EmailService; // ✅ ADDED
//import com.aja.internshipportal.service.SubscriptionService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.time.temporal.ChronoUnit;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class SubscriptionServiceImpl implements SubscriptionService {
//
//    private final SubscriptionRepository subscriptionRepository;
//    private final UserRepository userRepository;
//    private final PackageRepository packageRepository;
//    private final EmailService emailService; // ✅ ADDED
//
//    // ── Get my subscriptions ──
//    @Override
//    @Transactional
//    public List<SubscriptionResponse> getMySubscriptions(String email) {
//        User user = getUserByEmail(email);
//
//        return subscriptionRepository.findByUser(user)
//                .stream()
//                .map(this::mapToSubscriptionResponse)
//                .collect(Collectors.toList());
//    }
//
//    // ── Create subscription after payment verified ──
//    @Override
//    @Transactional
//    public void createSubscription(Payment payment,
//                                   CoursePackage coursePackage,
//                                   Tier tier) {
//
//        LocalDate startDate = LocalDate.now();
//
//        // calculate end date based on tier
//        LocalDate endDate = switch (tier) {
//            case BASIC   -> startDate.plusDays(30);
//            case STANDARD -> startDate.plusDays(90);
//            case PREMIUM  -> startDate.plusDays(180);
//            case BUNDLE   -> startDate.plusDays(180);
//        };
//
//        Subscription subscription = Subscription.builder()
//                .user(payment.getUser())
//                .aPackage(coursePackage)
//                .tier(tier)
//                .startDate(startDate)
//                .endDate(endDate)
//                .status(Subscription.SubscriptionStatus.ACTIVE)
//                .payment(payment)
//                .build();
//
//        subscriptionRepository.save(subscription);
//
//        // ✅ INTEGRATION: Automated Subscription Activation Email
//        try {
//            emailService.sendSubscriptionConfirmationEmail(
//                    payment.getUser().getEmail(),
//                    payment.getUser().getFullName(),
//                    coursePackage != null ? coursePackage.getName() : "Premium Package",
//                    tier.name(),
//                    endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
//            );
//            log.info("Subscription confirmation email sent to: {}", payment.getUser().getEmail());
//        } catch (Exception e) {
//            log.error("Failed to send subscription confirmation email for user: {}", payment.getUser().getEmail(), e);
//        }
//    }
//
//    // ── Check active subscription ──
//    @Override
//    public boolean hasActiveSubscription(String email, Long packageId) {
//
//        User user = getUserByEmail(email);
//
//        CoursePackage coursePackage = packageRepository.findById(packageId)
//                .orElseThrow(() ->
//                    AppException.notFound("Package not found")
//                );
//
//        // check specific package subscription
//        boolean hasSpecific = subscriptionRepository
//                .findActiveSubscription(user, coursePackage, LocalDate.now())
//                .isPresent();
//
//        if (hasSpecific) return true;
//
//        // check if user has bundle subscription
//        return subscriptionRepository
//                .findAllActiveSubscriptions(user, LocalDate.now())
//                .stream()
//                .anyMatch(sub ->
//                    sub.getAPackage() != null && 
//                    sub.getAPackage().getPackageType() == CoursePackage.PackageType.BUNDLE
//                );
//    }
//
//    // ── helper ──
//
//    private User getUserByEmail(String email) {
//        return userRepository.findByEmail(email)
//                .orElseThrow(() ->
//                    AppException.notFound("User not found")
//                );
//    }
//
//    private SubscriptionResponse mapToSubscriptionResponse(
//            Subscription subscription) {
//
//        // calculate days remaining
//        long daysRemaining = ChronoUnit.DAYS.between(
//                LocalDate.now(),
//                subscription.getEndDate()
//        );
//
//        // if expired set to 0
//        daysRemaining = Math.max(daysRemaining, 0);
//
//        return SubscriptionResponse.builder()
//                .id(subscription.getId())
//                .packageName(subscription.getAPackage() != null ? subscription.getAPackage().getName() : "General Package")
//                .tier(subscription.getTier())
//                .startDate(subscription.getStartDate())
//                .endDate(subscription.getEndDate())
//                .status(subscription.getStatus())
//                .daysRemaining(daysRemaining)
//                .createdAt(subscription.getCreatedAt())
//                .build();
//    }
//}

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
import com.aja.internshipportal.service.EmailService;
import com.aja.internshipportal.service.SmsService; // ✅ ADDED
import com.aja.internshipportal.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final PackageRepository packageRepository;
    private final EmailService emailService;
    private final SmsService smsService; // ✅ ADDED

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

        // ✅ INTEGRATION: Automated Subscription Activation Email
        try {
            emailService.sendSubscriptionConfirmationEmail(
                    payment.getUser().getEmail(),
                    payment.getUser().getFullName(),
                    coursePackage != null ? coursePackage.getName() : "Premium Package",
                    tier.name(),
                    endDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
            );
            log.info("Subscription confirmation email sent to: {}", payment.getUser().getEmail());
        } catch (Exception e) {
            log.error("Failed to send subscription confirmation email for user: {}", payment.getUser().getEmail(), e);
        }

        // ✅ INTEGRATION: Automated Payment Success SMS
        try {
            if (payment.getUser().getPhone() != null && !payment.getUser().getPhone().isEmpty()) {
                smsService.sendPaymentSuccessSms(
                        payment.getUser().getPhone(),
                        payment.getUser().getFullName(),
                        coursePackage != null ? coursePackage.getName() : "Premium Package",
                        payment.getAmount().toString()
                );
                log.info("Payment success SMS sent to: {}", payment.getUser().getPhone());
            }
        } catch (Exception e) {
            log.error("Failed to send payment success SMS for user: {}", payment.getUser().getPhone(), e);
        }
    }

    // ── Check active subscription ──
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
        return subscriptionRepository
                .findAllActiveSubscriptions(user, LocalDate.now())
                .stream()
                .anyMatch(sub ->
                    sub.getAPackage() != null && 
                    sub.getAPackage().getPackageType() == CoursePackage.PackageType.BUNDLE
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
                .packageName(subscription.getAPackage() != null ? subscription.getAPackage().getName() : "General Package")
                .tier(subscription.getTier())
                .startDate(subscription.getStartDate())
                .endDate(subscription.getEndDate())
                .status(subscription.getStatus())
                .daysRemaining(daysRemaining)
                .createdAt(subscription.getCreatedAt())
                .build();
    }
}

