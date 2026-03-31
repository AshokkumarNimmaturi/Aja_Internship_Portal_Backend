package com.aja.internshipportal.controller;

import com.aja.internshipportal.dto.request.CreateUserRequest;
import com.aja.internshipportal.dto.request.UpdateUserRequest;
import com.aja.internshipportal.dto.response.ApiResponse;
import com.aja.internshipportal.dto.response.UserResponse;
import com.aja.internshipportal.entity.AuditLog;
import com.aja.internshipportal.service.AuditLogService;
import com.aja.internshipportal.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // entire controller — ADMIN only
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final AuditLogService auditLogService; // ✅ ADDED

    // ── CREATE USER ──
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.createuser(request));
    }

    // ── GET ALL USERS ──
    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // ── UPDATE USER ──
    @PutMapping("/users/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    // ── DEACTIVATE USER ──
    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.ok(
                ApiResponse.success("User deactivated successfully")
        );
    }

    // ── AUDIT LOGS ──
    // GET /api/admin/audit-log?page=0&size=20
    @GetMapping("/audit-log")
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("createdAt").descending()
        );

        return ResponseEntity.ok(
                auditLogService.getAuditLogs(pageable)
        );
    }
}