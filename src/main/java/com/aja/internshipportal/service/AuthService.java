package com.aja.internshipportal.service;

import com.aja.internshipportal.dto.request.ChangePasswordRequest;
import com.aja.internshipportal.dto.request.ForgotPasswordRequest;
import com.aja.internshipportal.dto.request.LoginRequest;
import com.aja.internshipportal.dto.request.RefreshTokenRequest;
import com.aja.internshipportal.dto.request.RegisterRequest;
import com.aja.internshipportal.dto.request.ResetPasswordRequest;
import com.aja.internshipportal.dto.response.ApiResponse;
import com.aja.internshipportal.dto.response.AuthResponse;

public interface AuthService {

	//SUBSCRIBER self registration
	AuthResponse register(RegisterRequest request);
	
    // All roles — returns accessToken + refreshToken + user info
    AuthResponse login(LoginRequest request);
    
    // Sends reset link to email
    ApiResponse forgotPassword(ForgotPasswordRequest request);
    
    //Validates token from email link and sets new password
    ApiResponse resetPassword(ResetPasswordRequest request);
    
 // Changes password — clears firstLogin flag for internal users
    ApiResponse changePassword(String email, ChangePasswordRequest request);

    // Validates refresh token — returns new access token
    AuthResponse refreshToken(RefreshTokenRequest request);
}
