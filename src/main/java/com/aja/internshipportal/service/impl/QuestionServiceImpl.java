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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final TechnologyRepository technologyRepository;
    private final AnswerRepository answerRepository;

    // ── Submit question ──
    @Override
    @Transactional
    public QuestionResponse submitQuestion(String email, QuestionRequest request) {

        User user = getUserByEmail(email);

        Technology technology = technologyRepository
                .findById(request.getTechnologyId())
                .orElseThrow(() -> AppException.notFound("Technology not found"));

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

        return mapToQuestionResponse(question);
    }

    // ── Get questions (FIXED 🔥) ──
    @Override
    @Transactional(readOnly = true) // ✅ IMPORTANT FIX
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

    // ── Get single question (FIXED 🔥) ──
    @Override
    @Transactional(readOnly = true) // ✅ IMPORTANT FIX
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

    // ── Helper ──
    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("User not found"));
    }

    // ── Mapper (SAFE VERSION 🔥) ──
    public QuestionResponse mapToQuestionResponse(Question question) {

        return QuestionResponse.builder()
                .id(question.getId())
                .title(question.getTitle())
                .content(question.getContent())

                // ✅ NULL SAFE
                .technologyName(
                        question.getTechnology() != null
                                ? question.getTechnology().getName()
                                : null
                )

                .status(question.getStatus())
                .difficulty(question.getDifficulty())
                .tags(question.getTags())
                .sample(question.isSample())

                // ✅ NULL SAFE
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