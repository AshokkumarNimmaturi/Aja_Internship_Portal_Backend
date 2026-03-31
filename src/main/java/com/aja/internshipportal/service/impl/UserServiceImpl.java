package com.aja.internshipportal.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aja.internshipportal.dto.request.CreateUserRequest;
import com.aja.internshipportal.dto.request.UpdateUserRequest;
import com.aja.internshipportal.dto.response.UserResponse;
import com.aja.internshipportal.entity.User;
import com.aja.internshipportal.exception.AppException;
import com.aja.internshipportal.repository.UserRepository;
import com.aja.internshipportal.service.EmailService;
import com.aja.internshipportal.service.PdfService;
import com.aja.internshipportal.service.UserService;
import com.aja.internshipportal.service.AuditLogService;
import com.aja.internshipportal.util.AuditActions;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PdfService pdfService;
    private final AuditLogService auditLogService; // ✅ ADDED

    // ── CREATE USER ──
    @Override
    public UserResponse createuser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw AppException.conflict("Email already registered with our database");
        }

        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw AppException.conflict("Phone number already registered");
        }

        if (request.getRole() == User.Role.ADMIN || request.getRole() == User.Role.SUBSCRIBER) {
            throw AppException.badRequest("Only TUTOR or EMPLOYEE accounts can be created here");
        }

        String tempPassword = generateTempPassword();

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(tempPassword))
                .phone(request.getPhone())
                .role(request.getRole())
                .enabled(true)
                .firstLogin(true)
                .build();

        userRepository.save(user);

        // ✅ AUDIT LOG
        auditLogService.log(user, AuditActions.USER_CREATED,
                "User", user.getId(),
                "Internal user created: " + user.getEmail() +
                        " role: " + user.getRole(),
                null
        );

        // send credentials email
        byte[] pdf = pdfService.generateCredentialsPdf(user, tempPassword);
        emailService.sendCredentialsEmail(
                user.getEmail(),
                user.getFullName(),
                tempPassword,
                pdf
        );

        return mapToUserResponse(user);
    }

    // ── GET ALL USERS ──
    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    // ── UPDATE USER ──
    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        userRepository.save(user);

        // ✅ AUDIT LOG
        auditLogService.log(user, AuditActions.USER_UPDATED,
                "User", user.getId(),
                "User updated: " + user.getEmail(),
                null
        );

        return mapToUserResponse(user);
    }

    // ── DEACTIVATE USER ──
    @Override
    @Transactional
    public void deactivateUser(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("User not found"));

        user.setEnabled(false);
        userRepository.save(user);

        // ✅ AUDIT LOG
        auditLogService.log(user, AuditActions.USER_DEACTIVATED,
                "User", user.getId(),
                "User deactivated: " + user.getEmail(),
                null
        );
    }

    // ── GET PROFILE ──
    @Override
    public UserResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("User not found"));
        return mapToUserResponse(user);
    }

    // ── HELPERS ──

    private String generateTempPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    public UserResponse mapToUserResponse(User user) {
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