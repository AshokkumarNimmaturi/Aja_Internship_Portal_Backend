package com.aja.internshipportal.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aja.internshipportal.dto.request.QuestionRequest;
import com.aja.internshipportal.dto.request.ReviewQuestionRequest;
import com.aja.internshipportal.dto.response.QuestionResponse;
import com.aja.internshipportal.entity.Question;
import com.aja.internshipportal.entity.Technology;
import com.aja.internshipportal.entity.User;
import com.aja.internshipportal.exception.AppException;
import com.aja.internshipportal.repository.AnswerRepository;
import com.aja.internshipportal.repository.QuestionRepository;
import com.aja.internshipportal.repository.TechnologyRepository;
import com.aja.internshipportal.repository.UserRepository;
import com.aja.internshipportal.service.QuestionService;
import com.aja.internshipportal.service.AuditLogService;
import com.aja.internshipportal.util.AuditActions;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final TechnologyRepository technologyRepository;
    private final AnswerRepository answerRepository;
    private final AuditLogService auditLogService; // ✅ ADDED

    // ── Submit question ──
    @Override
    @Transactional
    public QuestionResponse submitQuestion(String email, QuestionRequest request) {

        User user = getUserByEmail(email);


     // new — uses helper that checks both camelCase and snake_case
     Technology technology = technologyRepository
             .findById(request.getResolvedTechnologyId())
             .orElseThrow(() ->
                 AppException.notFound("Technology not found")
             );
        Question question = Question.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .technology(technology)
                .submittedBy(user)
                .difficulty(request.getDifficulty())
                .tags(request.getTags())
                .status(Question.Status.PENDING)
                .sample(false)
                .build();

        questionRepository.save(question);

        // ✅ AUDIT LOG
        auditLogService.log(user, AuditActions.QUESTION_SUBMITTED,
                "Question", question.getId(),
                "Question submitted: " + question.getTitle(),
                null
        );

        return mapToQuestionResponse(question);
    }

    // ── Review question ──
    @Override
    @Transactional
    public QuestionResponse reviewQuestion(Long id, String email,
                                           ReviewQuestionRequest request) {

        Question question = questionRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Question not found"));

        if (question.getStatus() != Question.Status.PENDING) {
            throw AppException.badRequest("Only PENDING questions can be reviewed");
        }

        if (request.getDecision() == Question.Status.REJECTED &&
                (request.getRejectionReason() == null ||
                 request.getRejectionReason().isBlank())) {
            throw AppException.badRequest("Rejection reason is required");
        }

        User reviewer = getUserByEmail(email);

        question.setStatus(request.getDecision());
        question.setReviewedBy(reviewer);
        question.setRejectionReason(request.getRejectionReason());

        questionRepository.save(question);

        // ✅ PICK ACTION DYNAMICALLY
        String action = request.getDecision() == Question.Status.APPROVED
                ? AuditActions.QUESTION_APPROVED
                : AuditActions.QUESTION_REJECTED;

        // ✅ AUDIT LOG
        auditLogService.log(reviewer, action,
                "Question", question.getId(),
                "Question " + request.getDecision().name().toLowerCase() +
                        ": " + question.getTitle(),
                null
        );

        return mapToQuestionResponse(question);
    }

    // ── Get questions ──
    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getQuestions(Long technologyId,
                                               String keyword,
                                               Pageable pageable) {

        if (technologyId != null) {
            Technology technology = technologyRepository.findById(technologyId)
                    .orElseThrow(() -> AppException.notFound("Technology not found"));

            if (keyword != null && !keyword.isBlank()) {
                return questionRepository
                        .searchByKeywordAndTechnology(
                                Question.Status.APPROVED,
                                technology,
                                keyword,
                                pageable)
                        .map(this::mapToQuestionResponse);
            }

            return questionRepository
                    .findByTechnologyAndStatus(
                            technology,
                            Question.Status.APPROVED,
                            pageable)
                    .map(this::mapToQuestionResponse);
        }

        if (keyword != null && !keyword.isBlank()) {
            return questionRepository
                    .searchByKeyword(
                            Question.Status.APPROVED,
                            keyword,
                            pageable)
                    .map(this::mapToQuestionResponse);
        }

        return questionRepository
                .findByStatus(Question.Status.APPROVED, pageable)
                .map(this::mapToQuestionResponse);
    }

    // ── Get single question ──
    @Override
    @Transactional(readOnly = true)
    public QuestionResponse getQuestionById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Question not found"));

        return mapToQuestionResponse(question);
    }

    // ── Sample questions ──
    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getSampleQuestions(Long technologyId) {

        Technology technology = technologyRepository.findById(technologyId)
                .orElseThrow(() -> AppException.notFound("Technology not found"));

        return questionRepository
                .findByTechnologyAndSampleTrue(technology)
                .stream()
                .limit(5)
                .map(this::mapToQuestionResponse)
                .collect(Collectors.toList());
    }

    // ── My questions ──
    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getMyQuestions(String email) {

        User user = getUserByEmail(email);

        return questionRepository.findBySubmittedBy(user)
                .stream()
                .map(this::mapToQuestionResponse)
                .collect(Collectors.toList());
    }
 // ── Pending questions (Admin/Tutor review queue) ──
    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getPendingQuestions(Pageable pageable) {

        return questionRepository
                .findByStatus(Question.Status.PENDING, pageable)
                .map(this::mapToQuestionResponse);
    }

    // ── Helper ──
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("User not found"));
    }

    // ── Mapper ──
    public QuestionResponse mapToQuestionResponse(Question question) {

        return QuestionResponse.builder()
                .id(question.getId())
                .title(question.getTitle())
                .content(question.getContent())
                .technologyName(
                        question.getTechnology() != null
                                ? question.getTechnology().getName()
                                : null
                )
                .status(question.getStatus())
                .difficulty(question.getDifficulty())
                .tags(question.getTags())
                .sample(question.isSample())
                .submittedByName(
                        question.getSubmittedBy() != null
                                ? question.getSubmittedBy().getFullName()
                                : null
                )
                .reviewedByName(
                        question.getReviewedBy() != null
                                ? question.getReviewedBy().getFullName()
                                : null
                )
                .rejectionReason(question.getRejectionReason())
                .answerCount(answerRepository.countByQuestion(question))
                .createdAt(question.getCreatedAt())
                .build();
    }
}