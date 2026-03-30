package com.aja.internshipportal.service.impl;

import com.aja.internshipportal.dto.request.PaymentOrderRequest;
import com.aja.internshipportal.dto.request.PaymentVerifyRequest;
import com.aja.internshipportal.dto.response.ApiResponse;
import com.aja.internshipportal.dto.response.PaymentOrderResponse;
import com.aja.internshipportal.entity.CoursePackage;
import com.aja.internshipportal.entity.Payment;
import com.aja.internshipportal.entity.Tier;
import com.aja.internshipportal.entity.User;
import com.aja.internshipportal.exception.AppException;
import com.aja.internshipportal.repository.PackageRepository;
import com.aja.internshipportal.repository.PaymentRepository;
import com.aja.internshipportal.repository.UserRepository;
import com.aja.internshipportal.service.PaymentService;
import com.aja.internshipportal.service.SubscriptionService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final PackageRepository packageRepository;
    private final SubscriptionService subscriptionService;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    // ── Step 1 — Create Razorpay order ──
    @Override
    @Transactional
    public PaymentOrderResponse createOrder(String email,
                                            PaymentOrderRequest request) {

        User user = getUserByEmail(email);

        CoursePackage coursePackage = packageRepository
                .findById(request.getPackageId())
                .orElseThrow(() ->
                    AppException.notFound("Package not found")
                );

        // calculate amount based on tier
        BigDecimal amount = calculateAmount(coursePackage, request.getTier());

        try {
            // create Razorpay client
            RazorpayClient razorpayClient = new RazorpayClient(
                razorpayKeyId,
                razorpayKeySecret
            );

            // Razorpay needs amount in paise (1 INR = 100 paise)
            int amountInPaise = amount.multiply(
                BigDecimal.valueOf(100)
            ).intValue();

            // build Razorpay order request
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "receipt_" + System.currentTimeMillis());

            // create order in Razorpay
            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            // save payment record in our DB with CREATED status
            Payment payment = Payment.builder()
                    .user(user)
                    .aPackage(coursePackage)
                    .tier(request.getTier())
                    .razorpayOrderId(razorpayOrderId)
                    .amount(amount)
                    .status(Payment.PaymentStatus.CREATED)
                    .build();

            paymentRepository.save(payment);

            // return order details to frontend
            // frontend uses these to open Razorpay checkout
            return PaymentOrderResponse.builder()
                    .razorpayOrderId(razorpayOrderId)
                    .amount(amount)
                    .currency("INR")
                    .razorpayKeyId(razorpayKeyId)
                    .build();

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw AppException.badRequest(
                "Payment order creation failed. Please try again."
            );
        }
    }

    // ── Step 2 — Verify payment signature ──
    // Razorpay sends 3 values after payment
    // we verify signature to confirm payment is genuine
    @Override
    @Transactional
    public ApiResponse verifyPayment(String email,
                                     PaymentVerifyRequest request) {

        // find payment by Razorpay order ID
        Payment payment = paymentRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() ->
                    AppException.notFound("Payment record not found")
                );

        // verify signature — prevents fake payment callbacks
        boolean isValid = verifyRazorpaySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!isValid) {
            // mark payment as failed
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw AppException.badRequest("Payment verification failed. Invalid signature.");
        }

        // signature valid — mark payment as success
        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        // create subscription for the user
        subscriptionService.createSubscription(
                payment,
                payment.getAPackage(),
                payment.getTier()
        );

        log.info("Payment verified successfully for user: {} order: {}",
                email, request.getRazorpayOrderId());

        return ApiResponse.success(
            "Payment successful. Your subscription is now active."
        );
    }

    // ── Razorpay signature verification ──
    // formula: HMAC-SHA256(orderId + "|" + paymentId, secret)
    // if result matches signature sent by Razorpay — payment is genuine
    private boolean verifyRazorpaySignature(String orderId,
                                             String paymentId,
                                             String signature) {
        try {
            String payload = orderId + "|" + paymentId;

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    razorpayKeySecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKey);

            byte[] hash = mac.doFinal(
                payload.getBytes(StandardCharsets.UTF_8)
            );

            // convert bytes to hex string
            String generatedSignature = toHexString(hash);

            return generatedSignature.equals(signature);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    // converts byte array to hex string
    private String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    // ── calculate amount based on package + tier ──
    private BigDecimal calculateAmount(CoursePackage pkg, Tier tier) {
        return switch (tier) {
            case BASIC    -> pkg.getBasicPrice();
            case STANDARD -> pkg.getStandardPrice();
            case PREMIUM  -> pkg.getPremiumPrice();
            case BUNDLE   -> pkg.getBundlePrice() != null
                                ? pkg.getBundlePrice()
                                : pkg.getPremiumPrice();
        };
    }

    // ── helper ──
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                    AppException.notFound("User not found")
                );
    }
}