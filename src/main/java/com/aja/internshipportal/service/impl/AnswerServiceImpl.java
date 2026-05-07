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

    // ── Get all answers for a question ──
    @Override
    @Transactional(readOnly = true)
    public List<AnswerResponse> getAnswersByQuestion(Long questionId,
                                                     String email) {

        Question question = getQuestionById(questionId);
        User currentUser = getUserByEmail(email);

        return answerRepository
                .findByQuestionOrderByUpvoteCountDesc(question)
                .stream()
                .map(answer -> mapToAnswerResponse(answer, currentUser))
                .collect(Collectors.toList());
    }

    // ✅ Implementation for fetching the user's specific answers
    @Override
    @Transactional(readOnly = true)
    public List<AnswerResponse> getMyAnswers(String email) {
        User user = getUserByEmail(email);
        return answerRepository.findByAuthorOrderByCreatedAtDesc(user)
                .stream()
                .map(answer -> mapToAnswerResponse(answer, user))
                .collect(Collectors.toList());
    }

    // ── Add answer ──
    @Override
    @Transactional
    public AnswerResponse addAnswer(Long questionId, String email,
                                   AnswerRequest request) {

        Question question = getQuestionById(questionId);
        User user = getUserByEmail(email);

        if (question.getStatus() != Question.Status.APPROVED) {
            throw AppException.badRequest(
                    "Answers can only be added to approved questions"
            );
        }

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

    // ── Toggle upvote ──
    @Override
    @Transactional
    public AnswerResponse toggleUpvote(Long answerId, String email) {

        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() ->
                        AppException.notFound("Answer not found")
                );

        User user = getUserByEmail(email);

        if (answer.getUpvotedByUserIds().contains(user.getId())) {

            // remove upvote
            answer.getUpvotedByUserIds().remove(user.getId());
            answer.setUpvoteCount(answer.getUpvoteCount() - 1);

        } else {

            // add upvote
            answer.getUpvotedByUserIds().add(user.getId());
            answer.setUpvoteCount(answer.getUpvoteCount() + 1);
        }

        answerRepository.save(answer);

        return mapToAnswerResponse(answer, user);
    }

    // ── Helpers ──

    private Question getQuestionById(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() ->
                        AppException.notFound("Question not found")
                );
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        AppException.notFound("User not found")
                );
    }

    // ── Mapper (SAFE VERSION 🔥) ──
    private AnswerResponse mapToAnswerResponse(Answer answer, User currentUser) {

        return AnswerResponse.builder()
                .id(answer.getId())
                .content(answer.getContent())

                // ✅ ADDED: This is what fixes the "View Discussion" link
                .questionId(
                        answer.getQuestion() != null 
                                ? answer.getQuestion().getId() 
                                : null
                )

                // ✅ Question title for the "My Answers" view
                .questionTitle(
                        answer.getQuestion() != null 
                                ? answer.getQuestion().getTitle() 
                                : "Unknown Question"
                )

                .authorName(
                        answer.getAuthor() != null
                                ? answer.getAuthor().getFullName()
                                : null
                )

                .upvoteCount(answer.getUpvoteCount())

                .upvotedByCurrentUser(
                        answer.getUpvotedByUserIds() != null &&
                        answer.getUpvotedByUserIds().contains(currentUser.getId())
                )

                .accepted(answer.isAccepted())
                .createdAt(answer.getCreatedAt())
                .build();
    }
}
