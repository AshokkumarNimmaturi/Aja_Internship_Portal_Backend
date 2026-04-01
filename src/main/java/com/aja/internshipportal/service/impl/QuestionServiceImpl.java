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
import com.aja.internshipportal.entity.Answer;
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
    private final AuditLogService auditLogService;

    // ── Submit question ──
    @Override
    @Transactional
    public QuestionResponse submitQuestion(String email, QuestionRequest request) {
        User user = getUserByEmail(email);
        Technology technology = technologyRepository
                .findById(request.getResolvedTechnologyId())
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

        if (request.getInitialAnswer() != null && !request.getInitialAnswer().isBlank()) {
            Answer answer = Answer.builder()
                    .question(question)
                    .author(user)
                    .content(request.getInitialAnswer())
                    .upvoteCount(0)
                    .accepted(false)
                    .build();
            answerRepository.save(answer);
        }

        auditLogService.log(user, AuditActions.QUESTION_SUBMITTED,
                "Question", question.getId(),
                "Question submitted with answer: " + question.getTitle(),
                null
        );

        return mapToQuestionResponse(question);
    }

    // ── Review question ──
    @Override
    @Transactional
    public QuestionResponse reviewQuestion(Long id, String email, ReviewQuestionRequest request) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Question not found"));

        if (question.getStatus() != Question.Status.PENDING) {
            throw AppException.badRequest("Only PENDING questions can be reviewed");
        }

        User reviewer = getUserByEmail(email);

        if (request.getDecision() == Question.Status.APPROVED) {
            List<Answer> answers = answerRepository.findByQuestionOrderByUpvoteCountDesc(question);
            if (!answers.isEmpty()) {
                Answer answer = answers.get(0);
                if (request.getCorrectedAnswer() != null && !request.getCorrectedAnswer().isBlank()) {
                    answer.setContent(request.getCorrectedAnswer());
                }
                answer.setAccepted(true);
                answerRepository.save(answer);
            }
        }

        question.setStatus(request.getDecision());
        question.setReviewedBy(reviewer);
        question.setRejectionReason(request.getRejectionReason());
        questionRepository.save(question);

        String action = request.getDecision() == Question.Status.APPROVED
                ? AuditActions.QUESTION_APPROVED
                : AuditActions.QUESTION_REJECTED;

        auditLogService.log(reviewer, action, "Question", question.getId(),
                "Question " + request.getDecision().name().toLowerCase() + ": " + question.getTitle(), null);

        return mapToQuestionResponse(question);
    }

    // ✅ ADDED: Update existing question logic
    @Override
    @Transactional
    public QuestionResponse updateQuestion(Long id, String email, QuestionRequest request) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Question not found"));
        User user = getUserByEmail(email);
        
        // Security Check
        boolean isAuthor = question.getSubmittedBy() != null && 
                          question.getSubmittedBy().getEmail().equals(email);
        boolean isPrivileged = user.getRole() == User.Role.ADMIN || 
                               user.getRole() == User.Role.TUTOR;
        
        if (!isAuthor && !isPrivileged) {
            throw AppException.forbidden("You don't have permission to edit this question");
        }

        // Apply Updates
        question.setTitle(request.getTitle());
        question.setContent(request.getContent());
        question.setDifficulty(request.getDifficulty());
        question.setTags(request.getTags());

        if (request.getResolvedTechnologyId() != null) {
            Technology technology = technologyRepository.findById(request.getResolvedTechnologyId())
                    .orElseThrow(() -> AppException.notFound("Technology not found"));
            question.setTechnology(technology);
        }

        questionRepository.save(question);

        // Map Audit Action (using string if constant isn't yet added to AuditActions.java)
        auditLogService.log(user, "QUESTION_UPDATED", "Question", question.getId(),
                "Question updated: " + question.getTitle(), null);

        return mapToQuestionResponse(question);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getQuestions(Long technologyId, String keyword, Pageable pageable) {
        if (technologyId != null) {
            Technology technology = technologyRepository.findById(technologyId)
                    .orElseThrow(() -> AppException.notFound("Technology not found"));
            if (keyword != null && !keyword.isBlank()) {
                return questionRepository.searchByKeywordAndTechnology(Question.Status.APPROVED, technology, keyword, pageable).map(this::mapToQuestionResponse);
            }
            return questionRepository.findByTechnologyAndStatus(technology, Question.Status.APPROVED, pageable).map(this::mapToQuestionResponse);
        }
        if (keyword != null && !keyword.isBlank()) {
            return questionRepository.searchByKeyword(Question.Status.APPROVED, keyword, pageable).map(this::mapToQuestionResponse);
        }
        return questionRepository.findByStatus(Question.Status.APPROVED, pageable).map(this::mapToQuestionResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionResponse getQuestionById(Long id) {
        return questionRepository.findById(id).map(this::mapToQuestionResponse).orElseThrow(() -> AppException.notFound("Question not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getSampleQuestions(Long technologyId) {
        Technology technology = technologyRepository.findById(technologyId).orElseThrow(() -> AppException.notFound("Technology not found"));
        return questionRepository.findByTechnologyAndSampleTrue(technology).stream().limit(5).map(this::mapToQuestionResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getMyQuestions(String email) {
        User user = getUserByEmail(email);
        return questionRepository.findBySubmittedBy(user).stream().map(this::mapToQuestionResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getPendingQuestions(Pageable pageable) {
        return questionRepository.findByStatus(Question.Status.PENDING, pageable).map(this::mapToQuestionResponse);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> AppException.notFound("User not found"));
    }

    public QuestionResponse mapToQuestionResponse(Question question) {
        String initialAnswer = null;
        try {
            List<Answer> answers = answerRepository.findByQuestionOrderByUpvoteCountDesc(question);
            if (!answers.isEmpty()) {
                initialAnswer = answers.get(0).getContent();
            }
        } catch (Exception e) {}

        return QuestionResponse.builder()
                .id(question.getId())
                .title(question.getTitle())
                .content(question.getContent())
                .initialAnswer(initialAnswer)
                .technologyName(question.getTechnology() != null ? question.getTechnology().getName() : null)
                .status(question.getStatus())
                .difficulty(question.getDifficulty())
                .tags(question.getTags())
                .sample(question.isSample())
                .submittedByName(question.getSubmittedBy() != null ? question.getSubmittedBy().getFullName() : null)
                .reviewedByName(question.getReviewedBy() != null ? question.getReviewedBy().getFullName() : null)
                .rejectionReason(question.getRejectionReason())
                .answerCount(answerRepository.countByQuestion(question))
                .createdAt(question.getCreatedAt())
                .build();
    }
}
