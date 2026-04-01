package com.aja.internshipportal.service;

import java.util.List;

import com.aja.internshipportal.dto.request.AnswerRequest;
import com.aja.internshipportal.dto.response.AnswerResponse;

public interface AnswerService {
	 
    // Get all answers for a question — ordered by upvotes
	List<AnswerResponse> getAnswersByQuestion(Long questionId, String email);
	
	// Add answer to a question
    AnswerResponse addAnswer(Long questionId, String email,
                             AnswerRequest request);
    
    // Toggle upvote — if already upvoted remove it, else add it
    AnswerResponse toggleUpvote(Long answerId, String email);

    // ✅ ADDED: Fetch all answers for the logged-in user
    List<AnswerResponse> getMyAnswers(String email);
}
