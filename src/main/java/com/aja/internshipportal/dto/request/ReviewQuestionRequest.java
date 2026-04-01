package com.aja.internshipportal.dto.request;

import com.aja.internshipportal.entity.Question;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewQuestionRequest {

	// APPROVED or REJECTED only
	private Question.Status decision;
	
	// Required only when decision = REJECTED
	private String rejectionReason;

    // ✅ ADDED: Allows Tutors/Admins to correct the answer during review
    private String correctedAnswer; 
}
