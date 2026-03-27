package com.aja.internshipportal.controller;

import com.aja.internshipportal.dto.request.AnswerRequest;
import com.aja.internshipportal.dto.response.AnswerResponse;
import com.aja.internshipportal.service.AnswerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    // GET /api/answers/{questionId} — get all answers for a question
    @GetMapping("/{questionId}")
    public ResponseEntity<List<AnswerResponse>> getAnswers(
            @PathVariable Long questionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
            answerService.getAnswersByQuestion(
                questionId,
                userDetails.getUsername()
            )
        );
    }

    // POST /api/answers/{questionId} — add answer
    @PostMapping("/{questionId}")
    public ResponseEntity<AnswerResponse> addAnswer(
            @PathVariable Long questionId,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AnswerRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(answerService.addAnswer(
                    questionId,
                    userDetails.getUsername(),
                    request
                ));
    }

    // PUT /api/answers/{id}/upvote — toggle upvote
    @PutMapping("/{id}/upvote")
    public ResponseEntity<AnswerResponse> toggleUpvote(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(
            answerService.toggleUpvote(id, userDetails.getUsername())
        );
    }
}