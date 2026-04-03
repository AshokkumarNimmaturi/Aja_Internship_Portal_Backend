package com.aja.internshipportal.controller;

import com.aja.internshipportal.dto.request.QuestionRequest;
import com.aja.internshipportal.dto.request.ReviewQuestionRequest;
import com.aja.internshipportal.dto.response.QuestionResponse;
import com.aja.internshipportal.service.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    public ResponseEntity<Page<QuestionResponse>> getQuestions(
            @RequestParam(required = false) Long technologyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(questionService.getQuestions(technologyId, keyword, pageable));
    }

    @GetMapping("/samples")
    public ResponseEntity<List<QuestionResponse>> getSamples(@RequestParam Long technologyId) {
        return ResponseEntity.ok(questionService.getSampleQuestions(technologyId));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('EMPLOYEE','TUTOR','ADMIN')")
    public ResponseEntity<List<QuestionResponse>> getMyQuestions(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(questionService.getMyQuestions(userDetails.getUsername()));
    }

    // ✅ NEW: GET /api/questions/recent — Fetch 5 most recently visited questions for dashboard
    @GetMapping("/recent")
    @PreAuthorize("hasRole('SUBSCRIBER')")
    public ResponseEntity<List<QuestionResponse>> getRecentQuestions(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(questionService.getRecentQuestions(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionResponse> getQuestionById(@PathVariable Long id) {
        return ResponseEntity.ok(questionService.getQuestionById(id));
    }

    // ✅ NEW: POST /api/questions/{id}/visit — Record a study visit
    @PostMapping("/{id}/visit")
    @PreAuthorize("hasRole('SUBSCRIBER')")
    public ResponseEntity<Void> recordVisit(
            @PathVariable Long id, 
            @AuthenticationPrincipal UserDetails userDetails) {
        questionService.recordVisit(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('TUTOR','ADMIN')")
    public ResponseEntity<Page<QuestionResponse>> getPendingQuestions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(questionService.getPendingQuestions(pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','TUTOR','ADMIN')")
    public ResponseEntity<QuestionResponse> submitQuestion(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(questionService.submitQuestion(userDetails.getUsername(), request));
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('TUTOR','ADMIN')")
    public ResponseEntity<QuestionResponse> reviewQuestion(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReviewQuestionRequest request) {
        return ResponseEntity.ok(questionService.reviewQuestion(id, userDetails.getUsername(), request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','TUTOR','ADMIN')")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody QuestionRequest request) {
        return ResponseEntity.ok(questionService.updateQuestion(id, userDetails.getUsername(), request));
    }
}
