package com.aja.internshipportal.dto.request;

import com.aja.internshipportal.entity.User;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

	@Size(min = 2, max=100)
	private String fullName;
	
	//Admin can change role
	private User.Role role;
	
	//Admin can activate/deactivate account
	private Boolean enabled;
}
