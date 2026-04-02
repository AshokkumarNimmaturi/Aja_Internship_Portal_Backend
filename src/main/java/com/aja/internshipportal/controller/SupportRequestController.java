// PATH: src/main/java/com/aja/internshipportal/controller/SupportRequestController.java

package com.aja.internshipportal.controller;

import com.aja.internshipportal.entity.SupportRequest;
import com.aja.internshipportal.repository.SupportRequestRepository;
import com.aja.internshipportal.service.EmailService;
import com.aja.internshipportal.entity.User;
import com.aja.internshipportal.repository.UserRepository;
import com.aja.internshipportal.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@RequiredArgsConstructor
public class SupportRequestController {

    private final SupportRequestRepository supportRequestRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @PostMapping
    public ResponseEntity<String> submitRequest(@RequestBody SupportRequest request) {
        // Automatically link user if logged in (optional but recommended)
        try {
            supportRequestRepository.save(request);
            // Alert Admin immediately
            emailService.sendSupportAlertToAdmin(request.getUserEmail(), request.getSubject(), request.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to submit request: " + e.getMessage());
        }
        return ResponseEntity.ok("Support request submitted successfully");
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SupportRequest>> getAllRequests() {
        return ResponseEntity.ok(supportRequestRepository.findAllByOrderByCreatedAtDesc());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateStatus(
            @PathVariable Long id,
            @RequestParam SupportRequest.RequestStatus status) { // ✅ Now matches Entity

        SupportRequest request = supportRequestRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Support request not found"));

        request.setStatus(status);
        supportRequestRepository.save(request);

        return ResponseEntity.ok("Status updated to " + status);
    }
}
