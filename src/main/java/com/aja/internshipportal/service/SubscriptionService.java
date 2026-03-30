package com.aja.internshipportal.service;

import java.util.List;

import com.aja.internshipportal.dto.response.SubscriptionResponse;
import com.aja.internshipportal.entity.CoursePackage;
import com.aja.internshipportal.entity.Payment;
import com.aja.internshipportal.entity.Tier;

public interface SubscriptionService {

    // Subscriber — get my active subscriptions
    List<SubscriptionResponse> getMySubscriptions(String email);

    // Called internally after payment success — creates subscription
    void createSubscription(Payment payment, CoursePackage coursePackage,
                            Tier tier);

    // Check if user has active subscription for a package
    // Used as access gate before serving questions
    boolean hasActiveSubscription(String email, Long packageId);
}