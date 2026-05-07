package com.aja.internshipportal.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aja.internshipportal.dto.response.SubscriptionResponse;
import com.aja.internshipportal.service.SubscriptionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    // GET /api/subscriptions/my — get my subscriptions
    @Transactional
    @GetMapping("/my")
    public ResponseEntity<List<SubscriptionResponse>> getMySubscriptions(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
            subscriptionService.getMySubscriptions(
                userDetails.getUsername()
            )
        );
    }
}