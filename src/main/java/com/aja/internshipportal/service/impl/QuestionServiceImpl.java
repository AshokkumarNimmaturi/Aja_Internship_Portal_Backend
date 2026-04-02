// PATH: src/main/java/com/aja/internshipportal/service/impl/QuestionServiceImpl.java

package com.aja.internshipportal.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aja.internshipportal.dto.request.QuestionRequest;
import com.aja.internshipportal.dto.request.ReviewQuestionRequest;
import com.aja.internshipportal.dto.response.QuestionResponse;
import com.aja.internshipportal.entity.*;
import com.aja.internshipportal.exception.AppException;
import com.aja.internshipportal.repository.*;
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
                .clientName(request.getClientName()) // ✅ Client tracking
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

        auditLogService.log(user, AuditActions.QUESTION_SUBMITTED, "Question", question.getId(), 
                "New submission: " + question.getTitle(), null);

        return mapToQuestionResponse(question);
    }

    @Override
    @Transactional
    public QuestionResponse reviewQuestion(Long id, String email, ReviewQuestionRequest request) {
        Question question = questionRepository.findById(id).orElseThrow(() -> AppException.notFound("Not found"));
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

        // ✅ SYNCED: Now matches AuditActions.QUESTION_REVIEWED
        auditLogService.log(reviewer, AuditActions.QUESTION_REVIEWED, "Question", question.getId(),
                "Decision for #" + id + ": " + request.getDecision(), null);

        return mapToQuestionResponse(question);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionResponse getQuestionById(Long id) {
        return questionRepository.findById(id).map(this::mapToQuestionResponse).orElseThrow(() -> AppException.notFound("Not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getQuestions(Long tid, String k, Pageable p) {
        return questionRepository.findByStatus(Question.Status.APPROVED, p).map(this::mapToQuestionResponse);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> AppException.notFound("User not found"));
    }

    public QuestionResponse mapToQuestionResponse(Question question) {
        String initialAnswer = null;
        try {
            List<Answer> answers = answerRepository.findByQuestionOrderByUpvoteCountDesc(question);
            if (!answers.isEmpty()) initialAnswer = answers.get(0).getContent();
        } catch (Exception e) {}

        // ✅ PRIVACY SHIELD: Protected Intel Layer
        String displayClient = question.getClientName();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            boolean redact = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUBSCRIBER") || a.getAuthority().equals("ROLE_EMPLOYEE"));
            if (redact) displayClient = "REDACTED (Premium Only)";
        }

        return QuestionResponse.builder()
                .id(question.getId())
                .title(question.getTitle())
                .content(question.getContent())
                .clientName(displayClient) 
                .initialAnswer(initialAnswer)
                .technologyName(question.getTechnology() != null ? question.getTechnology().getName() : null)
                .status(question.getStatus())
                .difficulty(question.getDifficulty())
                .tags(question.getTags())
                .submittedByName(question.getSubmittedBy() != null ? question.getSubmittedBy().getFullName() : null)
                .createdAt(question.getCreatedAt())
                .answerCount(answerRepository.countByQuestion(question))
                .build();
    }
    
    // --- Legacy Support ---
    @Override @Transactional(readOnly = true)
    public List<QuestionResponse> getSampleQuestions(Long tid) { return questionRepository.findByTechnologyAndSampleTrue(technologyRepository.findById(tid).get()).stream().map(this::mapToQuestionResponse).collect(Collectors.toList()); }
    
    @Override @Transactional(readOnly = true)
    public List<QuestionResponse> getMyQuestions(String e) { return questionRepository.findBySubmittedBy(getUserByEmail(e)).stream().map(this::mapToQuestionResponse).collect(Collectors.toList()); }
    
    @Override @Transactional(readOnly = true)
    public Page<QuestionResponse> getPendingQuestions(Pageable p) { return questionRepository.findByStatus(Question.Status.PENDING, p).map(this::mapToQuestionResponse); }

    @Override @Transactional
    public QuestionResponse updateQuestion(Long id, String email, QuestionRequest request) {
        Question question = questionRepository.findById(id).orElseThrow(() -> AppException.notFound("Not found"));
        question.setTitle(request.getTitle());
        question.setContent(request.getContent());
        question.setClientName(request.getClientName());
        questionRepository.save(question);
        return mapToQuestionResponse(question);
    }
}
