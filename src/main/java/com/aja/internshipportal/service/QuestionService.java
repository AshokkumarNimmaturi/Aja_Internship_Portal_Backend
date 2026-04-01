package com.aja.internshipportal.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aja.internshipportal.dto.request.QuestionRequest;
import com.aja.internshipportal.dto.request.ReviewQuestionRequest;
import com.aja.internshipportal.dto.response.QuestionResponse;

public interface QuestionService {

	//EMPLOYEE/TUTOR/ADMIN  -  submit a new question
	QuestionResponse submitQuestion(String email, QuestionRequest request);
	
	 // TUTOR/ADMIN — approve or reject a question
    QuestionResponse reviewQuestion(Long id, String email,
                                    ReviewQuestionRequest request);
    

    // All authenticated users — paginated list with filters
    Page<QuestionResponse> getQuestions(Long technologyId,
                                        String keyword,
                                        Pageable pageable);
    
    // All authenticated users — get single question
    QuestionResponse getQuestionById(Long id);
    
    // Public — 5 free samples per technology
    List<QuestionResponse> getSampleQuestions(Long technologyId);
    
    // EMPLOYEE — my submitted questions
    List<QuestionResponse> getMyQuestions(String email);
    
    Page<QuestionResponse> getPendingQuestions(Pageable pageable);
}
