package com.aja.internshipportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentVerifyRequest {

	 // All 3 fields come from Razorpay after successful payment
    @NotBlank(message = "Order ID is required")
	private String razorpayOrderId;
	
    @NotBlank(message = "Payment ID is required")
	private String razorpayPaymentId;
	
    @NotBlank(message = "Signature is required")
	private String razorpaySignature;
}
