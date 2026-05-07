package com.aja.internshipportal.controller;

import com.aja.internshipportal.dto.request.PaymentOrderRequest;
import com.aja.internshipportal.dto.request.PaymentVerifyRequest;
import com.aja.internshipportal.dto.response.ApiResponse;
import com.aja.internshipportal.dto.response.PaymentOrderResponse;
import com.aja.internshipportal.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
@PreAuthorize("hasRole('SUBSCRIBER')") // only subscribers can pay
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // POST /api/payment/create-order
    // subscriber clicks Buy Now → frontend calls this
    // returns Razorpay order details to open checkout
    @PostMapping("/create-order")
    public ResponseEntity<PaymentOrderResponse> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PaymentOrderRequest request) {
        return ResponseEntity.ok(
            paymentService.createOrder(
                userDetails.getUsername(),
                request
            )
        );
    }

    // POST /api/payment/verify
    // called after Razorpay checkout completes
    // verifies signature and activates subscription
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verifyPayment(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PaymentVerifyRequest request) {
        return ResponseEntity.ok(
            paymentService.verifyPayment(
                userDetails.getUsername(),
                request
            )
        );
    }
}