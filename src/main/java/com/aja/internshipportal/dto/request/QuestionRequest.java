package com.aja.internshipportal.dto.request;

import com.aja.internshipportal.entity.Question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionRequest {

	@NotBlank(message="Title is required")
	@Size(min = 10, max = 1000, message = "Title must be between 10 and 1000 characters")
	private String title;
	
	@NotBlank(message="Content is required")
	private String content;
	
	@NotNull(message="Technology is required")
	private Long technologyId;
	
	@NotNull(message = "Difficulty is required")
	private Question.Difficulty difficulty;
	
	//Optional coma-separated tags
	private String tags;
}
