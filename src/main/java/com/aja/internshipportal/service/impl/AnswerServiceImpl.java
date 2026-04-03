// PATH: src/main/java/com/aja/internshipportal/service/impl/AnswerServiceImpl.java
// ELITE SYNC: Removed status restrictions. Internal contributions are now enabled at all stages.

package com.aja.internshipportal.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aja.internshipportal.dto.request.AnswerRequest;
import com.aja.internshipportal.dto.response.AnswerResponse;
import com.aja.internshipportal.entity.Answer;
import com.aja.internshipportal.entity.Question;
import com.aja.internshipportal.entity.User;
import com.aja.internshipportal.exception.AppException;
import com.aja.internshipportal.repository.AnswerRepository;
import com.aja.internshipportal.repository.QuestionRepository;
import com.aja.internshipportal.repository.UserRepository;
import com.aja.internshipportal.service.AnswerService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnswerServiceImpl implements AnswerService {

    private final AnswerRepository answerRepository;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AnswerResponse> getAnswersByQuestion(Long questionId, String email) {
        Question question = getQuestionById(questionId);
        User currentUser = getUserByEmail(email);

        return answerRepository
                .findByQuestionOrderByUpvoteCountDesc(question)
                .stream()
                .map(answer -> mapToAnswerResponse(answer, currentUser))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnswerResponse> getMyAnswers(String email) {
        User user = getUserByEmail(email);
        return answerRepository.findByAuthorOrderByCreatedAtDesc(user)
                .stream()
                .map(answer -> mapToAnswerResponse(answer, user))
                .collect(Collectors.toList());
    }

    /**
     * ✅ ELITE SYNC: Intelligence contributions are now allowed regardless of status
     * so that Tutors can refine answers during the review phase.
     */
    @Override
    @Transactional
    public AnswerResponse addAnswer(Long questionId, String email, AnswerRequest request) {
        Question question = getQuestionById(questionId);
        User user = getUserByEmail(email);

        // SYNC REMOVAL: status != APPROVED check removed for high-speed curation.

        Answer answer = Answer.builder()
                .question(question)
                .author(user)
                .content(request.getContent())
                .upvoteCount(0)
                .accepted(false)
                .build();

        answerRepository.save(answer);
        return mapToAnswerResponse(answer, user);
    }

    @Override
    @Transactional
    public AnswerResponse toggleUpvote(Long answerId, String email) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> AppException.notFound("Answer not found"));
        User user = getUserByEmail(email);

        if (answer.getUpvotedByUserIds().contains(user.getId())) {
            answer.getUpvotedByUserIds().remove(user.getId());
            answer.setUpvoteCount(answer.getUpvoteCount() - 1);
        } else {
            answer.getUpvotedByUserIds().add(user.getId());
            answer.setUpvoteCount(answer.getUpvoteCount() + 1);
        }

        answerRepository.save(answer);
        return mapToAnswerResponse(answer, user);
    }

    private Question getQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Question not found"));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> AppException.notFound("User not found"));
    }

    private AnswerResponse mapToAnswerResponse(Answer answer, User currentUser) {
        return AnswerResponse.builder()
                .id(answer.getId())
                .content(answer.getContent())
                .questionId(answer.getQuestion() != null ? answer.getQuestion().getId() : null)
                .questionTitle(answer.getQuestion() != null ? answer.getQuestion().getTitle() : "Unknown")
                .authorName(answer.getAuthor() != null ? answer.getAuthor().getFullName() : "Anonymous")
                .upvoteCount(answer.getUpvoteCount())
                .upvotedByCurrentUser(answer.getUpvotedByUserIds() != null && answer.getUpvotedByUserIds().contains(currentUser.getId()))
                .accepted(answer.isAccepted())
                .createdAt(answer.getCreatedAt())
                .build();
    }
}
