package com.aja.internshipportal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aja.internshipportal.entity.Payment;
import com.aja.internshipportal.entity.User;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

	// Find payment by Razorpay order ID — used during verify step
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    
    // Find payment by Razorpay payment ID — used for duplicate check
    Optional<Payment> findByRazorpayPaymentId(String razorpayPaymentId);
    
    // Payment history for a user — admin or user profile
    List<Payment> findByUserOrderByCreatedAtDesc(User user);
    
    // All payments with a given status — admin dashboard
    List<Payment> findByStatus(Payment.PaymentStatus status);
}
