package com.aja.internshipportal.service.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aja.internshipportal.dto.request.*;
import com.aja.internshipportal.dto.response.*;
import com.aja.internshipportal.entity.*;
import com.aja.internshipportal.exception.AppException;
import com.aja.internshipportal.repository.*;
import com.aja.internshipportal.security.JwtUtil;
import com.aja.internshipportal.service.*;
import com.aja.internshipportal.util.AuditActions;

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
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    // ── REGISTER ──
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw AppException.conflict("Email already registered");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw AppException.conflict("Phone number already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(User.Role.SUBSCRIBER)
                .enabled(true)
                .firstLogin(false)
                .build();

        userRepository.save(user);

        // ✅ AUDIT LOG
        auditLogService.log(user, AuditActions.USER_REGISTERED,
                "User", user.getId(),
                "New subscriber registered: " + user.getEmail(),
                null
        );

        return buildAuthResponse(user);
    }

    // ── LOGIN ──
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> AppException.notFound("User Not Found"));

        // ✅ AUDIT LOG
        auditLogService.log(user, AuditActions.USER_LOGIN,
                "User", user.getId(),
                "User logged in: " + user.getEmail(),
                null
        );

        return buildAuthResponse(user);
    }

    // ── FORGOT PASSWORD ──
    @Override
    @Transactional
    public ApiResponse forgotPassword(ForgotPasswordRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> AppException.notFound("No account found with this email"));

        passwordResetTokenRepository.deleteByUser(user);

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .used(false)
                .build();

        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(
                user.getEmail(),
                user.getFullName(),
                token
        );

        return ApiResponse.success("Password reset link sent to " + user.getEmail());
    }

    // ── RESET PASSWORD ──
    @Override
    @Transactional
    public ApiResponse resetPassword(ResetPasswordRequest request) {

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw AppException.badRequest("Passwords do not match");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> AppException.badRequest("Invalid reset token"));

        if (resetToken.isExpired()) {
            throw AppException.badRequest("Reset token has expired");
        }

        if (resetToken.isUsed()) {
            throw AppException.badRequest("Reset token already used");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        // ✅ AUDIT LOG
        auditLogService.log(user, AuditActions.PASSWORD_RESET,
                "User", user.getId(),
                "Password reset for: " + user.getEmail(),
                null
        );

        // ✅ EMAIL
        emailService.sendPasswordChangedEmail(
                user.getEmail(),
                user.getFullName()
        );

        return ApiResponse.success("Password reset successful");
    }

    // ── CHANGE PASSWORD ──
    @Override
    @Transactional
    public ApiResponse changePassword(String email, ChangePasswordRequest request) {

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw AppException.badRequest("Passwords do not match");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (!user.isFirstLogin()) {
            if (request.getCurrentPassword() == null ||
                    !passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw AppException.badRequest("Current password incorrect");
            }
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setFirstLogin(false);
        userRepository.save(user);

        // ✅ AUDIT LOG
        auditLogService.log(user, AuditActions.PASSWORD_CHANGED,
                "User", user.getId(),
                "Password changed for: " + user.getEmail(),
                null
        );

        return ApiResponse.success("Password changed successfully");
    }

    // ── REFRESH TOKEN ──
    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> AppException.unauthorized("Invalid refresh token"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw AppException.unauthorized("Refresh token expired");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtUtil.generateAccessToken(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .tokenType("Bearer")
                .user(mapToUserResponse(user))
                .build();
    }

    // ── HELPERS ──

    private AuthResponse buildAuthResponse(User user) {

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshTokenStr = jwtUtil.generateRefreshToken(user);

        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElse(RefreshToken.builder().user(user).build());

        refreshToken.setToken(refreshTokenStr);
        refreshToken.setExpiryDate(Instant.now().plusMillis(604800000));

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .user(mapToUserResponse(user))
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .firstLogin(user.isFirstLogin())
                .profilePicture(user.getProfilePicture())
                .createdAt(user.getCreatedAt())
                .build();
    }
}