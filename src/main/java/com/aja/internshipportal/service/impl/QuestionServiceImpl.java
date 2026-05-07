package com.aja.internshipportal.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final SubscriptionRepository subscriptionRepository; // ✅ ADDED
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
                .clientName(request.getClientName())
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

    // ✅ NEW: Added getRecentQuestions to fix the 500 Dashboard error
    @Override
    @Transactional(readOnly = true)
    public List<QuestionResponse> getRecentQuestions(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by("createdAt").descending());
        // We reuse the getQuestions logic to ensure the Subscriber sees only their authorized tech
        return getQuestions(null, null, pageable).getContent();
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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSubscriber = auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUBSCRIBER"));

        // ✅ LOGIC FIX: Check subscription access for subscribers
        if (isSubscriber) {
            User user = getUserByEmail(auth.getName());
            List<Subscription> activeSubscriptions = subscriptionRepository.findAllActiveSubscriptions(user, LocalDate.now());
            
            if (activeSubscriptions.isEmpty()) {
                return Page.empty(); // No active premium found
            }

            // Check if user has a BUNDLE (full access)
            boolean hasBundle = activeSubscriptions.stream()
                    .anyMatch(s -> s.getAPackage() != null && s.getAPackage().getPackageType() == CoursePackage.PackageType.BUNDLE);

            if (!hasBundle) {
                // Get specifically subscribed technologies
                List<Technology> subscribedTechs = activeSubscriptions.stream()
                        .map(s -> s.getAPackage() != null ? s.getAPackage().getTechnology() : null)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toList());

                // If filtering by a specific technology, ensure they own it
                if (tid != null) {
                    Technology requestedTech = technologyRepository.findById(tid).orElse(null);
                    if (requestedTech == null || !subscribedTechs.contains(requestedTech)) {
                        return Page.empty();
                    }
                    return questionRepository.findByTechnologyAndStatus(requestedTech, Question.Status.APPROVED, p)
                            .map(this::mapToQuestionResponse);
                }
                
                // Otherwise return all questions from their subscribed technologies
                return questionRepository.findByTechnologyInAndStatus(subscribedTechs, Question.Status.APPROVED, p)
                        .map(this::mapToQuestionResponse);
            }
        }

        // For Staff (Admin/Tutor/Employee) or Bundle Subscribers
        if (tid != null) {
            Technology technology = technologyRepository.findById(tid).orElse(null);
            if (technology != null) {
                return questionRepository.findByTechnologyAndStatus(technology, Question.Status.APPROVED, p)
                        .map(this::mapToQuestionResponse);
            }
        }
        
        return questionRepository.findByStatus(Question.Status.APPROVED, p).map(this::mapToQuestionResponse);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> AppException.notFound("User not found"));
    }

    @Override
    public QuestionResponse mapToQuestionResponse(Question question) {
        String initialAnswer = null;
        try {
            List<Answer> answers = answerRepository.findByQuestionOrderByUpvoteCountDesc(question);
            if (!answers.isEmpty()) initialAnswer = answers.get(0).getContent();
        } catch (Exception e) {}

        String displayClient = question.getClientName();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            boolean isSubscriber = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SUBSCRIBER"));
            
            if (isSubscriber) displayClient = "CONFIDENTIAL (Premium)"; // Professional Redaction
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
                .submittedByEmail(question.getSubmittedBy() != null ? question.getSubmittedBy().getEmail() : null)
                .createdAt(question.getCreatedAt())
                .answerCount(answerRepository.countByQuestion(question))
                .build();
    }
    
    @Override @Transactional(readOnly = true)
    public List<QuestionResponse> getSampleQuestions(Long tid) { 
        return questionRepository.findByTechnologyAndSampleTrue(technologyRepository.findById(tid).get()).stream()
                .map(this::mapToQuestionResponse).collect(Collectors.toList()); 
    }
    
    @Override @Transactional(readOnly = true)
    public List<QuestionResponse> getMyQuestions(String e) { 
        return questionRepository.findBySubmittedBy(getUserByEmail(e)).stream()
                .map(this::mapToQuestionResponse).collect(Collectors.toList()); 
    }
    
    @Override @Transactional(readOnly = true)
    public Page<QuestionResponse> getPendingQuestions(Pageable p) { 
        return questionRepository.findByStatus(Question.Status.PENDING, p).map(this::mapToQuestionResponse); 
    }

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
