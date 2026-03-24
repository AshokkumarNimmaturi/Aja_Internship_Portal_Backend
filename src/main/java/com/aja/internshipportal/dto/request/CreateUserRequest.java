package com.aja.internshipportal.dto.request;

import com.aja.internshipportal.entity.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUserRequest {

	
	@NotBlank(message="Full name is required")
	@Size(min=2, max=100)
	private String fullName;
	
	@NotBlank(message="Email is required")
	@Email(message="Enter a valid email address")
	private String email;
	
	@Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
	private String phone;
	
	// Only TUTOR or EMPLOYEE can be created by admin
	private User.Role role;
}
