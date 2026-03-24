package com.aja.internshipportal.dto.request;

import com.aja.internshipportal.entity.Tier;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentOrderRequest {

	 @NotNull(message = "Package ID is required")
	private Long packageId;
	
	 // BASIC, STANDARD, PREMIUM, BUNDLE
    @NotNull(message = "Tier is required")
	private Tier tier;
}
