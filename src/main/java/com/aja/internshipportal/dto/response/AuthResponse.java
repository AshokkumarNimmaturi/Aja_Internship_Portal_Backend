package com.aja.internshipportal.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthResponse {

	//Short-lived token - sent with every API request
	private String accessToken;
	
	//Long-lived token - used to get new access token
	private String refreshToken;
	
	//Token type - always "Bearer"
	private String tokenType = "Bearer";
	
	//Full user info - frontend uses this to build the UI
	private UserResponse user;
}
