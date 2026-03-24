package com.aja.internshipportal.dto.response;

import java.time.LocalDateTime;

import com.aja.internshipportal.entity.Question;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class QuestionReponse {

	private Long id;
	private String title;
	private String content;
	private Question.Status status;
	private Question.Difficulty difficulty;
	private String tags;
	private boolean sample;
	
	//Submitted by info
	private String submittedByName;
	
	// Review info
	private String reviewedByName;
	private String rejectionReason;
	
	// Answer Count - shown on question list card
	private long answerCount;
	
	private LocalDateTime createdAt;
}
