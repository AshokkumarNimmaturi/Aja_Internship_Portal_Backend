package com.aja.internshipportal.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aja.internshipportal.entity.CoursePackage;
import com.aja.internshipportal.entity.Subscription;
import com.aja.internshipportal.entity.User;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
	// My subscriptions page — all subscriptions for logged-in user
    List<Subscription> findByUser(User user);
    
    // Check if user has active subscription for a specific package
    // Used as access gate before serving questions
    @Query("""
        SELECT s FROM Subscription s
        WHERE s.user = :user
        AND s.aPackage = :pkg
        AND s.status = 'ACTIVE'
        AND s.endDate >= :today
        """)
    Optional<Subscription> findActiveSubscription(
            @Param("user") User user,
            @Param("pkg") CoursePackage coursePackage,
            @Param("today") LocalDate today
    );
    
    // Check if user has any active subscription (for bundle access)
    @Query("""
        SELECT s FROM Subscription s
        WHERE s.user = :user
        AND s.status = 'ACTIVE'
        AND s.endDate >= :today
        """)
    List<Subscription> findAllActiveSubscriptions(
            @Param("user") User user,
            @Param("today") LocalDate today
    );
    
    // Scheduler job — find all subscriptions that expired today
    // (for auto-expiry background task later)
    List<Subscription> findByStatusAndEndDateBefore(
            Subscription.SubscriptionStatus status,
            LocalDate date
    );
}
