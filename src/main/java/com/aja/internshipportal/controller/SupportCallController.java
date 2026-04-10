package com.aja.internshipportal.controller;

import com.aja.internshipportal.entity.SupportCall;
import com.aja.internshipportal.repository.SupportCallRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/support-calls")
@RequiredArgsConstructor
public class SupportCallController {

    private final SupportCallRepository supportCallRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SupportCall>> getRecentCalls() {
        // Exposes the call history to the new table in your Admin Dashboard
        return ResponseEntity.ok(supportCallRepository.findTop20ByOrderByTimestampDesc());
    }
}
