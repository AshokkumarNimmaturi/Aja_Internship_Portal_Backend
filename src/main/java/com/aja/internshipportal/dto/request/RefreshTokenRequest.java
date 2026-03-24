package com.aja.internshipportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {
	 // Frontend sends this when access token expires
	@NotBlank(message="Refresh token is required")
	private String refreshToken;
}
