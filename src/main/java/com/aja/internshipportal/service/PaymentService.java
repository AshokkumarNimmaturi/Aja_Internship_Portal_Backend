package com.aja.internshipportal.service;

import com.aja.internshipportal.dto.request.PaymentOrderRequest;
import com.aja.internshipportal.dto.request.PaymentVerifyRequest;
import com.aja.internshipportal.dto.response.ApiResponse;
import com.aja.internshipportal.dto.response.PaymentOrderResponse;

public interface PaymentService {

    // Step 1 — create Razorpay order
    // called when subscriber clicks Buy Now
    PaymentOrderResponse createOrder(String email,
                                     PaymentOrderRequest request);

    // Step 2 — verify payment signature
    // called after Razorpay checkout completes
    ApiResponse verifyPayment(String email,
                              PaymentVerifyRequest request);
}