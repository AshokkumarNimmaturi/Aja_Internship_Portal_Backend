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
import com.aja.internshipportal.service.*;
import com.aja.internshipportal.util.AuditActions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // ✅ ADDED

@Service
@RequiredArgsConstructor
@Slf4j // ✅ ADDED
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PdfService pdfService;
    private final AuditLogService auditLogService;

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
                .available(false) // Default to offline
                .inCall(false)
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
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    // ── UPDATE USER (ADMIN) ──
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

        // ✅ ADDED: Allow admin to update phone number
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getInterests() != null) {
            user.setInterests(request.getInterests());
        }

        userRepository.save(user);

        // ✅ AUDIT LOG
        auditLogService.log(user, AuditActions.USER_UPDATED,
                "User", user.getId(),
                "User updated by admin: " + user.getEmail(),
                null
        );

        return mapToUserResponse(user);
    }

    // ── UPDATE MY PROFILE ──
    @Override
    @Transactional
    public UserResponse updateMyProfile(String email, UpdateUserRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        // ✅ ADDED: Allow user to update their own phone number
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        if (request.getInterests() != null) {
            user.setInterests(request.getInterests());
        }

        userRepository.save(user);

        // ✅ AUDIT LOG
        auditLogService.log(user, AuditActions.USER_UPDATED,
                "User", user.getId(),
                "Self-profile updated: " + user.getEmail(),
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
    
    // ── ACTIVATE USER ──
    @Override
    @Transactional
    public void activateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("User not found"));
        user.setEnabled(true);
        userRepository.save(user);
        // ✅ AUDIT LOG
        auditLogService.log(user, AuditActions.USER_ACTIVATED,
                "User", user.getId(),
                "User access re-enabled: " + user.getEmail(),
                null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("User not found"));
        return mapToUserResponse(user);
    }

    // ✅ NEW: TOGGLE AVAILABILITY
    @Override
    @Transactional
    public UserResponse toggleAvailability(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("User not found"));
        
        user.setAvailable(!user.isAvailable());
        userRepository.save(user);
        
        log.info("Agent {} changed availability to: {}", user.getEmail(), user.isAvailable());
        return mapToUserResponse(user);
    }

    // ✅ NEW: SET IN-CALL STATUS
    @Override
    @Transactional
    public void setInCallStatus(String phone, boolean inCall) {
        // Numbers from Twilio can have +91, we need to match carefully
        String cleanedPhone = phone.replaceAll("[^0-9]", "");
        if (cleanedPhone.length() > 10) {
            cleanedPhone = cleanedPhone.substring(cleanedPhone.length() - 10);
        }
        
        final String finalPhone = cleanedPhone;
        userRepository.findAll().stream()
            .filter(u -> u.getPhone() != null && u.getPhone().contains(finalPhone))
            .findFirst()
            .ifPresent(u -> {
                u.setInCall(inCall);
                userRepository.save(u);
                log.info("Agent {} call status updated to: {}", u.getEmail(), inCall);
            });
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
                .available(user.isAvailable()) // ✅ ADDED
                .inCall(user.isInCall())       // ✅ ADDED
                .profilePicture(user.getProfilePicture())
                .interests(user.getInterests()) 
                .createdAt(user.getCreatedAt())
                .build();
    }
}
