package com.aja.internshipportal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

	// Required for normal password change (not first login)
	private String currentPassword;
	
	@NotBlank(message="New password is required")
	@Size(min=6,max=100,message = "Password must be at least 6 characters")
	private String newPassword;
	
	@NotBlank(message="Please confirm your new password")
	private String confirmPassword;
}
