package com.aja.internshipportal.dto.response;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class PaymentOrderResponse {
    // Returned to frontend — used to open Razorpay checkout
    private String razorpayOrderId;
    private BigDecimal amount;
    private String currency; 

    // Razorpay Key ID — frontend needs this to init the SDK
    private String razorpayKeyId;
    
}
