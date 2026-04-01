package com.aja.internshipportal.dto.request;

import com.aja.internshipportal.entity.Question;
import com.fasterxml.jackson.annotation.JsonProperty;

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
	 // camelCase — used by Java backend
    private Long technologyId;

    // snake_case — also accepted from frontend
    // if technologyId is null, use this one
    @JsonProperty("technology_id")
    private Long technology_id;
	
 // camelCase package id — ADD THIS
    private Long packageId;

    // snake_case package id — ADD THIS
    @JsonProperty("package_id")
    private Long package_id;
    
	@NotNull(message = "Difficulty is required")
	private Question.Difficulty difficulty;
	
	//Optional coma-separated tags
	private String tags;
	
	  // helper — returns whichever is not null
    // service calls this instead of getTechnologyId() directly
    public Long getResolvedTechnologyId() {
        return technologyId != null ? technologyId : technology_id;
    }

    public Long getResolvedPackageId() {
        return packageId != null ? packageId : package_id;
    }
}
