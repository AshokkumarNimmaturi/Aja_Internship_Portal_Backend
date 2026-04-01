package com.aja.internshipportal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aja.internshipportal.entity.Answer;
import com.aja.internshipportal.entity.Question;
import com.aja.internshipportal.entity.User;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    // Question detail page — load all answers for a question
    // ordered by upvotes descending (best answers first)
    List<Answer> findByQuestionOrderByUpvoteCountDesc(Question question);
    
    // ✅ ADDED: Fetch all answers by a specific user for the "My Answers" page
    List<Answer> findByAuthorOrderByCreatedAtDesc(User author);
	
    // Check if user already answered this question
    boolean existsByQuestionAndAuthor(Question question, User author);
    
    // Find specific answer by question + author (for edit)
    Optional<Answer> findByQuestionAndAuthor(Question question, User author);
    
    // Count answers for a question — shown on question list card
    long countByQuestion(Question question);
}
