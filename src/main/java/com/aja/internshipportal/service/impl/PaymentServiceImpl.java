package com.aja.internshipportal.service.impl;

import com.aja.internshipportal.dto.request.PaymentOrderRequest;
import com.aja.internshipportal.dto.request.PaymentVerifyRequest;
import com.aja.internshipportal.dto.response.ApiResponse;
import com.aja.internshipportal.dto.response.PaymentOrderResponse;
import com.aja.internshipportal.entity.CoursePackage;
import com.aja.internshipportal.entity.Payment;
import com.aja.internshipportal.entity.Tier;
import com.aja.internshipportal.entity.User;
import com.aja.internshipportal.entity.AuditLog; // Ensure correct import
import com.aja.internshipportal.exception.AppException;
import com.aja.internshipportal.repository.PackageRepository;
import com.aja.internshipportal.repository.PaymentRepository;
import com.aja.internshipportal.repository.UserRepository;
import com.aja.internshipportal.service.*;
import com.aja.internshipportal.util.AuditActions;
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
    private final AuditLogService auditLogService;
    private final InvoiceService invoiceService;
    private final EmailService emailService;
    private final SmsService smsService; // ✅ INTEGRATED

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    // ── CREATE ORDER ──
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

        BigDecimal amount = calculateAmount(coursePackage, request.getTier());

        try {
            RazorpayClient razorpayClient = new RazorpayClient(
                    razorpayKeyId,
                    razorpayKeySecret
            );

            int amountInPaise = amount.multiply(BigDecimal.valueOf(100)).intValue();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "receipt_" + System.currentTimeMillis());

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            Payment payment = Payment.builder()
                    .user(user)
                    .aPackage(coursePackage)
                    .tier(request.getTier())
                    .razorpayOrderId(razorpayOrderId)
                    .amount(amount)
                    .status(Payment.PaymentStatus.CREATED)
                    .build();

            paymentRepository.save(payment);

            // ✅ AUDIT LOG
            auditLogService.log(user, AuditActions.PAYMENT_ORDER_CREATED,
                    "Payment", payment.getId(),
                    "Payment order created: " + razorpayOrderId +
                            " amount: " + amount,
                    null
            );

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

    // ── VERIFY PAYMENT ──
    @Override
    @Transactional
    public ApiResponse verifyPayment(String email,
                                     PaymentVerifyRequest request) {

        Payment payment = paymentRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() ->
                        AppException.notFound("Payment record not found")
                );

        boolean isValid = verifyRazorpaySignature(
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );

        if (!isValid) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);

            // ✅ AUDIT LOG (FAILED)
            auditLogService.log(payment.getUser(), AuditActions.PAYMENT_FAILED,
                    "Payment", payment.getId(),
                    "Payment failed: invalid signature for order: " +
                            request.getRazorpayOrderId(),
                    null
            );

            throw AppException.badRequest("Payment verification failed. Invalid signature.");
        }

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        // ✅ AUDIT LOG (SUCCESS)
        auditLogService.log(payment.getUser(), AuditActions.PAYMENT_SUCCESS,
                "Payment", payment.getId(),
                "Payment verified: " + request.getRazorpayPaymentId(),
                null
        );

        // ✅ INTEGRATION: Automated Invoice PDF & Email
        try {
            byte[] invoicePdf = invoiceService.generateInvoicePdf(payment);
            emailService.sendInvoiceEmail(
                payment.getUser().getEmail(), 
                payment.getUser().getFullName(), 
                invoicePdf, 
                payment.getRazorpayOrderId(),
                payment.getAPackage() != null ? payment.getAPackage().getName() : "Premium Package",
                payment.getTier().name()
            );
            log.info("Invoice email sent to: {}", payment.getUser().getEmail());
        } catch (Exception e) {
            log.error("Failed to generate/send invoice", e);
        }

        // ✅ NEW: Automated Payment Success SMS
        try {
            if (payment.getUser().getPhone() != null && !payment.getUser().getPhone().isEmpty()) {
                smsService.sendPaymentSuccessSms(
                    payment.getUser().getPhone(),
                    payment.getUser().getFullName(),
                    payment.getAPackage() != null ? payment.getAPackage().getName() : "Technology Package",
                    payment.getAmount().toString()
                );
                log.info("Payment success SMS sent to: {}", payment.getUser().getPhone());
            }
        } catch (Exception e) {
            log.warn("Payment success SMS failed but subscription will proceed: {}", e.getMessage());
        }

        // ✅ CREATE SUBSCRIPTION
        subscriptionService.createSubscription(
                payment,
                payment.getAPackage(),
                payment.getTier()
        );

        log.info("Payment verified successfully for user: {} order: {}",
                email, request.getRazorpayOrderId());

        return ApiResponse.success(
                "Payment successful. Your subscription is now active. Invoice and confirmation have been sent via Email and SMS."
        );
    }

    // ── SIGNATURE VERIFY ──
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

            String generatedSignature = toHexString(hash);

            return generatedSignature.equals(signature);

        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private String toHexString(byte[] bytes) {
        Formatter formatter = new Formatter();
        for (byte b : bytes) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    private BigDecimal calculateAmount(CoursePackage pkg, Tier tier) {
        return switch (tier) {
            case BASIC -> pkg.getBasicPrice();
            case STANDARD -> pkg.getStandardPrice();
            case PREMIUM -> pkg.getPremiumPrice();
            case BUNDLE -> pkg.getBundlePrice() != null
                    ? pkg.getBundlePrice()
                    : pkg.getPremiumPrice();
        };
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        AppException.notFound("User not found")
                );
    }
}
