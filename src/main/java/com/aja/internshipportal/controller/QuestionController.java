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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
public class QuestionController {

    private final QuestionService questionService;

    // GET /api/questions?technologyId=1&keyword=spring&page=0&size=10
    // authenticated users — subscribers see full content
    @GetMapping
    public ResponseEntity<Page<QuestionResponse>> getQuestions(
            @RequestParam(required = false) Long technologyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(
            page, size, Sort.by("createdAt").descending()
        );

        return ResponseEntity.ok(
            questionService.getQuestions(technologyId, keyword, pageable)
        );
    }

    // GET /api/questions/samples?technologyId=1 — public
    @GetMapping("/samples")
    public ResponseEntity<List<QuestionResponse>> getSamples(
            @RequestParam Long technologyId) {
        return ResponseEntity.ok(
            questionService.getSampleQuestions(technologyId)
        );
    }

    // GET /api/questions/my — my submitted questions
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('EMPLOYEE','TUTOR','ADMIN')")
    public ResponseEntity<List<QuestionResponse>> getMyQuestions(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
            questionService.getMyQuestions(userDetails.getUsername())
        );
    }

    // GET /api/questions/{id}
    @GetMapping("/{id}")
    public ResponseEntity<QuestionResponse> getQuestionById(
            @PathVariable Long id) {
        return ResponseEntity.ok(questionService.getQuestionById(id));
    }

    // POST /api/questions — submit question
    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLOYEE','TUTOR','ADMIN')")
    public ResponseEntity<QuestionResponse> submitQuestion(
            @AuthenticationPrincipal UserDetails userDetails,
            //@AuthenticationPrincipal :It gives you:"Currently logged-in user details"
            @Valid @RequestBody QuestionRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(questionService.submitQuestion(
                    userDetails.getUsername(), request
                ));
    }

    // PUT /api/questions/{id}/review — TUTOR/ADMIN only
    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('TUTOR','ADMIN')")
    public ResponseEntity<QuestionResponse> reviewQuestion(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ReviewQuestionRequest request) {
        return ResponseEntity.ok(
            questionService.reviewQuestion(
                id, userDetails.getUsername(), request
            )
        );
    }
}
