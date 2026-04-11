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
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final SmsService smsService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // ✅ 1. Check for Duplicate Email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw AppException.conflict("Email already exists");
        }

        // ✅ 2. Check for Duplicate Phone (NEW)
        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw AppException.conflict("Phone number already exists");
            }
        }

        log.info("DEBUG: Registration attempt - Name: {}, Email: {}, Phone: {}", 
                 request.getFullName(), request.getEmail(), request.getPhone());

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(User.Role.SUBSCRIBER)
                .enabled(true)
                .build();

        userRepository.save(user);

        // ✅ INTEGRATION: Automated Welcome Email
        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getFullName());
        } catch (Exception e) {
            log.error("Email failed: {}", e.getMessage());
        }
        
        // ✅ INTEGRATION: Automated Welcome SMS
        try {
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                log.info("DEBUG: Attempting to send Welcome SMS to {}", user.getPhone());
                smsService.sendWelcomeSms(user.getPhone(), user.getFullName());
            } else {
                log.warn("DEBUG: No phone number provided for user {}, skipping SMS.", user.getEmail());
            }
        } catch (Exception e) {
            log.error("SMS trigger failed: {}", e.getMessage());
        }

        auditLogService.log(user, AuditActions.USER_REGISTERED, "User", user.getId(), "Subscriber registered", null);
        return buildAuthResponse(user);
    }

    @Override @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> AppException.notFound("Not Found"));
        auditLogService.log(user, AuditActions.USER_LOGIN, "User", user.getId(), "Logged in", null);
        return buildAuthResponse(user);
    }

    @Override @Transactional
    public ApiResponse forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() -> AppException.notFound("Not found"));
        passwordResetTokenRepository.deleteByUser(user);
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder().user(user).token(token).expiryDate(LocalDateTime.now().plusMinutes(15)).used(false).build();
        passwordResetTokenRepository.save(resetToken);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token);
        return ApiResponse.success("Email sent");
    }

    @Override @Transactional
    public ApiResponse resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) throw AppException.badRequest("No match");
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken()).orElseThrow(() -> AppException.badRequest("Invalid"));
        if (resetToken.isExpired() || resetToken.isUsed()) throw AppException.badRequest("Token error");
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        emailService.sendPasswordChangedEmail(user.getEmail(), user.getFullName());
        return ApiResponse.success("Reset successful");
    }

    @Override @Transactional
    public ApiResponse changePassword(String email, ChangePasswordRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> AppException.notFound("Not found"));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ApiResponse.success("Changed successfully");
    }

    @Override @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken()).orElseThrow(() -> AppException.unauthorized("Invalid"));
        User user = refreshToken.getUser();
        String newAccessToken = jwtUtil.generateAccessToken(user);
        return AuthResponse.builder().accessToken(newAccessToken).refreshToken(request.getRefreshToken()).tokenType("Bearer").user(mapToUserResponse(user)).build();
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshTokenStr = jwtUtil.generateRefreshToken(user);
        RefreshToken refreshToken = refreshTokenRepository.findByUser(user).orElse(RefreshToken.builder().user(user).build());
        refreshToken.setToken(refreshTokenStr);
        refreshToken.setExpiryDate(Instant.now().plusMillis(604800000));
        refreshTokenRepository.save(refreshToken);
        return AuthResponse.builder().accessToken(accessToken).refreshToken(refreshTokenStr).tokenType("Bearer").user(mapToUserResponse(user)).build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder().id(user.getId()).fullName(user.getFullName()).email(user.getEmail()).phone(user.getPhone()).role(user.getRole()).enabled(user.isEnabled()).createdAt(user.getCreatedAt()).build();
    }
}
