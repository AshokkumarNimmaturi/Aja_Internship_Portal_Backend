package com.aja.internshipportal.repository;

import com.aja.internshipportal.entity.Question;
import com.aja.internshipportal.entity.QuestionVisit;
import com.aja.internshipportal.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionVisitRepository extends JpaRepository<QuestionVisit, Long> {

    // ✅ Finds all visits for a specific user and question (to update timestamp)
    Optional<QuestionVisit> findByUserAndQuestion(User user, Question question);

    // ✅ Fetches the 5 most recently visited questions for a user
    List<QuestionVisit> findTop5ByUserOrderByVisitedAtDesc(User user);
}
