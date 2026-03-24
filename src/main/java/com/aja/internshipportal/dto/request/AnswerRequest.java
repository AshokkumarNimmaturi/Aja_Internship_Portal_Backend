package com.aja.internshipportal.dto.request;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnswerRequest {

	@NotBlank(message = "Answer content is required")
	private String content;
}
