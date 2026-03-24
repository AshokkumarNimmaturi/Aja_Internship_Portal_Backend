package com.aja.internshipportal.repository;

import org.springframework.data.domain.Pageable; // Use this instead
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aja.internshipportal.entity.Question;
import com.aja.internshipportal.entity.Technology;
import com.aja.internshipportal.entity.User;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    // Question list page — paginated, filter by technology + status
    Page<Question> findByTechnologyAndStatus(
            Technology technology,
            Question.Status status,
            Pageable pageable
    );
    
    // Question list page — filter by status only (all technologies)
    Page<Question> findByStatus(Question.Status status, Pageable pageable);
    
 // Tutor review panel — all pending questions
 // Rename this for the Tutor review panel
    List<Question> findAllByStatus(Question.Status status);
    
    // Employee — my submitted questions
    List<Question> findBySubmittedBy(User user);
    
    // Free samples page — 5 per technology
    List<Question> findByTechnologyAndSampleTrue(Technology technology);
    
    // Search by keyword in title or content
    @Query("""
        SELECT q FROM Question q
        WHERE q.status = :status
        AND (LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(q.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<Question> searchByKeyword(
            @Param("status") Question.Status status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
    
    
    // Search with technology filter
    @Query("""
        SELECT q FROM Question q
        WHERE q.status = :status
        AND q.technology = :technology
        AND (LOWER(q.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
        OR LOWER(q.content) LIKE LOWER(CONCAT('%', :keyword, '%')))
        """)
    Page<Question> searchByKeywordAndTechnology(
            @Param("status") Question.Status status,
            @Param("technology") Technology technology,
            @Param("keyword") String keyword,
            Pageable pageable
    );
    
    // Count questions per technology — for admin dashboard
    long countByTechnologyAndStatus(Technology technology, Question.Status status);
}
