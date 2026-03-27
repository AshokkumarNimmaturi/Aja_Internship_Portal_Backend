package com.aja.internshipportal.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aja.internshipportal.dto.request.ChangePasswordRequest;
import com.aja.internshipportal.dto.request.ForgotPasswordRequest;
import com.aja.internshipportal.dto.request.LoginRequest;
import com.aja.internshipportal.dto.request.RefreshTokenRequest;
import com.aja.internshipportal.dto.request.RegisterRequest;
import com.aja.internshipportal.dto.request.ResetPasswordRequest;
import com.aja.internshipportal.dto.response.ApiResponse;
import com.aja.internshipportal.dto.response.AuthResponse;
import com.aja.internshipportal.dto.response.UserResponse;
import com.aja.internshipportal.entity.PasswordResetToken;
import com.aja.internshipportal.entity.RefreshToken;
import com.aja.internshipportal.entity.User;
import com.aja.internshipportal.exception.AppException;
import com.aja.internshipportal.repository.PasswordResetTokenRepository;
import com.aja.internshipportal.repository.RefreshTokenRepository;
import com.aja.internshipportal.repository.UserRepository;
import com.aja.internshipportal.security.JwtUtil;
import com.aja.internshipportal.service.AuthService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtUtil jwtUtil;
	private final AuthenticationManager authenticationManager;

	// REGISTER ---SUBSCRIBER ONLY

	@Override
	@Transactional
	public AuthResponse register(RegisterRequest request) {
		// check duplicate email
		if (userRepository.existsByEmail(request.getEmail())) {
			throw AppException.conflict("Email already registered");
		}

		// check duplicate phone if provided
		if (userRepository.existsByPhone(request.getPhone())) {
			throw AppException.conflict("Phone number already registered");
		}

		// build and save user
		User user = User.builder().fullName(request.getFullName()).email(request.getEmail())
				.password(passwordEncoder.encode(request.getPassword())).phone(request.getPhone())
				.role(User.Role.SUBSCRIBER).enabled(true).firstLogin(false) // subscribers don't need forced change
				.build();

		userRepository.save(user);
		// generate tokens and return
		return buildAuthResponse(user);

	}

	// - LOGIN - all roles

	@Override
	@Transactional
	public AuthResponse login(LoginRequest request) {
		// Spring security verifies email + password
		// throws BadCredentialsException if wrong - caught by GlobalExceptionHandler
		authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

		// load user from DB
		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> AppException.notFound("User Not Found"));
		// generate tokens and return
		return buildAuthResponse(user);
	}

	// - FORGOT PASSWORD - sends reset email
	@Override
	@Transactional
	public ApiResponse forgotPassword(ForgotPasswordRequest request) {
		// Spring security verifies email + password
		// throws BadCredentialsException if wrong - caught by GlobalException

		User user = userRepository.findByEmail(request.getEmail())
				.orElseThrow(() -> AppException.notFound("No account founf wuth this email"));
		// delete olf tokens for this user
		passwordResetTokenRepository.deleteByUser(user);

		// generate new UUID token
		String token = UUID.randomUUID().toString();

		// save token - expires in 15 minutes
		PasswordResetToken resetToken = PasswordResetToken.builder().user(user).token(token)
				.expiryDate(LocalDateTime.now().plusMinutes(15)).used(false).build();
		passwordResetTokenRepository.save(resetToken);

		// TODO — send email with reset link
		// emailService.sendPasswordResetEmail(user.getEmail(), token);
		// we wire this after EmailService is built

		return ApiResponse.success("Password reset link sent to " + user.getEmail());
	}

	@Override
	@Transactional
	public ApiResponse resetPassword(ResetPasswordRequest request) {
		// validates password match
		if (!request.getNewPassword().equals(request.getConfirmPassword())) {
			throw AppException.badRequest("Password do not match");
		}

		// find token
		PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
				.orElseThrow(() -> AppException.badRequest("Invalid reset token"));

		// check not expired
		if (resetToken.isExpired()) {
			throw AppException.badRequest("Reset token has expired. Please request a new one");
		}

		// check not already used
		if (resetToken.isUsed()) {
			throw AppException.badRequest("Reset token has already been used");
		}

		// Now Update Password
		User user = resetToken.getUser();
		user.setPassword(passwordEncoder.encode(request.getNewPassword()));
		userRepository.save(user);

		// mark token as used
		resetToken.setUsed(true);
		passwordResetTokenRepository.save(resetToken);

		return ApiResponse.success("Password reset successfully. Please login.");
	}

	@Override
	@Transactional
	public ApiResponse changePassword(String email, ChangePasswordRequest request) {
		// validate passwords match
		if (!request.getNewPassword().equals(request.getConfirmPassword())) {
			throw AppException.badRequest("Password do not match");
		}

		User user = userRepository.findByEmail(email).orElseThrow(() -> AppException.notFound("User not found"));

		// if NOT firstLogin - verify current password
		if (!user.isFirstLogin()) {
			if (request.getCurrentPassword() == null
					|| !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
				throw AppException.badRequest("Current password is incorrect");
			}
		}

		// update password
		user.setPassword(passwordEncoder.encode(request.getNewPassword()));

		// clear firstLogin flag - internal users land here on first login
		user.setFirstLogin(false);
		userRepository.save(user);
		return ApiResponse.success("Password changed successfully");
	}

	@Override
	@Transactional
	public AuthResponse refreshToken(RefreshTokenRequest request) {
		// find refresh token in DB
		RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
				.orElseThrow(() -> AppException.unauthorized("Invalid refresh token"));

		// check not expired
		if (refreshToken.isExpired()) {
			refreshTokenRepository.delete(refreshToken);
			throw AppException.unauthorized("Refresh token expired. Please login again");
		}
		
		 // generate new access token only
        User user = refreshToken.getUser();
        String newAccessToken = jwtUtil.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .tokenType("Bearer")
                .user(mapToUserResponse(user))
                .build();
    }


	// ── private helpers ──

	// builds full AuthResponse with both tokens + user info
	private AuthResponse buildAuthResponse(User user) {

		String accessToken = jwtUtil.generateAccessToken(user);
		String refreshTokenStr = jwtUtil.generateRefreshToken(user);

		// save or replace refresh token in DB
		// one token per user — old one replaced on every login
		RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
				.orElse(RefreshToken.builder().user(user).build());

		refreshToken.setToken(refreshTokenStr);
		refreshToken.setExpiryDate(Instant.now().plusMillis(604800000) // 7 days
		);

		refreshTokenRepository.save(refreshToken);

		return AuthResponse.builder().accessToken(accessToken).refreshToken(refreshTokenStr).tokenType("Bearer")
				.user(mapToUserResponse(user)).build();
	}

	// converts User entity to UserResponse DTO
	private UserResponse mapToUserResponse(User user) {
		return UserResponse.builder().id(user.getId()).fullName(user.getFullName()).email(user.getEmail())
				.phone(user.getPhone()).role(user.getRole()).enabled(user.isEnabled()).firstLogin(user.isFirstLogin())
				.profilePicture(user.getProfilePicture()).createdAt(user.getCreatedAt()).build();
	}

}
