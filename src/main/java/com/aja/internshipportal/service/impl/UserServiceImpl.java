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
import com.aja.internshipportal.entity.SupportCall;
import com.aja.internshipportal.exception.AppException;
import com.aja.internshipportal.repository.UserRepository;
import com.aja.internshipportal.repository.SupportCallRepository;
import com.aja.internshipportal.service.*;
import com.aja.internshipportal.util.AuditActions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SupportCallRepository supportCallRepository;
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
                .status(User.SupportStatus.OFFLINE)
                .inCall(false)
                .build();

        userRepository.save(user);

        auditLogService.log(user, AuditActions.USER_CREATED, "User", user.getId(),
                "Internal user created: " + user.getEmail() + " role: " + user.getRole(), null);

        byte[] pdf = pdfService.generateCredentialsPdf(user, tempPassword);
        emailService.sendCredentialsEmail(user.getEmail(), user.getFullName(), tempPassword, pdf);

        return mapToUserResponse(user);
    }

    // ── GET ALL USERS ──
    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(this::mapToUserResponse).collect(Collectors.toList());
    }

    // ── UPDATE USER (ADMIN) ──
    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id).orElseThrow(() -> AppException.notFound("User not found"));
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getRole() != null) user.setRole(request.getRole());
        if (request.getEnabled() != null) user.setEnabled(request.getEnabled());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getInterests() != null) user.setInterests(request.getInterests());
        userRepository.save(user);

        auditLogService.log(user, AuditActions.USER_UPDATED, "User", user.getId(),
                "User updated by admin: " + user.getEmail(), null);
        return mapToUserResponse(user);
    }

    // ── UPDATE MY PROFILE ──
    @Override
    @Transactional
    public UserResponse updateMyProfile(String email, UpdateUserRequest request) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> AppException.notFound("User not found"));
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getPhone() != null) user.setPhone(request.getPhone());
        if (request.getInterests() != null) user.setInterests(request.getInterests());
        userRepository.save(user);

        auditLogService.log(user, AuditActions.USER_UPDATED, "User", user.getId(),
                "Self-profile updated: " + user.getEmail(), null);
        return mapToUserResponse(user);
    }

    // ── DEACTIVATE USER ──
    @Override
    @Transactional
    public void deactivateUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> AppException.notFound("User not found"));
        user.setEnabled(false);
        userRepository.save(user);
        auditLogService.log(user, AuditActions.USER_DEACTIVATED, "User", user.getId(),
                "User deactivated: " + user.getEmail(), null);
    }
    
    // ── ACTIVATE USER ──
    @Override
    @Transactional
    public void activateUser(Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> AppException.notFound("User not found"));
        user.setEnabled(true);
        userRepository.save(user);
        auditLogService.log(user, AuditActions.USER_ACTIVATED, "User", user.getId(),
                "User access re-enabled: " + user.getEmail(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> AppException.notFound("User not found"));
        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse updateSupportStatus(String email, User.SupportStatus status) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> AppException.notFound("User not found"));
        user.setStatus(status);
        userRepository.save(user);
        log.info("Agent {} changed status to: {}", user.getEmail(), status);
        return mapToUserResponse(user);
    }

    // ✅ DIAGNOSTIC VERSION: SET IN-CALL STATUS
    @Override
    @Transactional
    public void setInCallStatus(String phone, boolean inCall) {
        log.info("[DIAGNOSTIC] setInCallStatus called. Phone: {}, Status: {}", phone, inCall);
        
        String suffix = phone.replaceAll("[^0-9]", "");
        if (suffix.length() > 10) {
            suffix = suffix.substring(suffix.length() - 10);
        }
        
        final String finalSuffix = suffix;
        log.info("[DIAGNOSTIC] Phone Suffix: {}", finalSuffix);

        userRepository.findByPhoneEndingWith(finalSuffix).ifPresentOrElse(u -> {
            u.setInCall(inCall);
            userRepository.save(u);
            log.info("[DIAGNOSTIC] SUCCESS: Agent {} inCall matched and saved as {}", u.getEmail(), inCall);
        }, () -> {
            log.warn("[DIAGNOSTIC] FAILED: No agent found in DB with phone suffix: {}", finalSuffix);
        });
    }

    // ✅ NO-FAIL VERSION: markCallAsAnswered with FUZZY FALLBACK
    @Override
    @Transactional
    public void markCallAsAnswered(String agentPhone, String callSid) {
        log.info("[DIAGNOSTIC] markCallAsAnswered. Agent: {}, SID: {}", agentPhone, callSid);
        
        String suffix = agentPhone.replaceAll("[^0-9]", "");
        if (suffix.length() > 10) {
            suffix = suffix.substring(suffix.length() - 10);
        }
        final String finalSuffix = suffix;

        userRepository.findByPhoneEndingWith(finalSuffix).ifPresentOrElse(agent -> {
            log.info("[DIAGNOSTIC] Agent identified: {}. Matching Call SID...", agent.getFullName());
            
            // 1. ATTEMPT PRECISE MATCH VIA SID
            supportCallRepository.findByCallSid(callSid).ifPresentOrElse(c -> {
                c.setStatus(SupportCall.CallStatus.ANSWERED);
                c.setAgentNumber(agent.getPhone());
                c.setAgentName(agent.getFullName());
                supportCallRepository.save(c);
                log.info("[DIAGNOSTIC] SUCCESS: Precise SID match for {}", callSid);
            }, () -> {
                // 2. FUZZY MATCH FALLBACK: Match most recent 'MISSED' (unresolved) call
                log.warn("[DIAGNOSTIC] Precise SID match failed. Trying Fuzzy Fallback...");
                supportCallRepository.findTop20ByOrderByTimestampDesc().stream()
                    .filter(call -> call.getStatus() == SupportCall.CallStatus.MISSED)
                    .findFirst()
                    .ifPresent(call -> {
                        call.setStatus(SupportCall.CallStatus.ANSWERED);
                        call.setAgentNumber(agent.getPhone());
                        call.setAgentName(agent.getFullName());
                        supportCallRepository.save(call);
                        log.info("[DIAGNOSTIC] SUCCESS: Matched most recent log via Fuzzy Fallback.");
                    });
            });
        }, () -> {
            log.error("[DIAGNOSTIC] FAILED: No agent found for phone suffix: {}", finalSuffix);
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
                .status(user.getStatus())
                .inCall(user.isInCall())
                .profilePicture(user.getProfilePicture())
                .interests(user.getInterests()) 
                .createdAt(user.getCreatedAt())
                .build();
    }
}
