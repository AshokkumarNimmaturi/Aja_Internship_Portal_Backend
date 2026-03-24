package com.aja.internshipportal.dto.response;

import java.time.LocalDateTime;

import com.aja.internshipportal.entity.User;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserResponse {

	private Long id;
	private String fullName;
	private String email;
	private String phone;
	private User.Role role;
	private boolean enabled;
	
	//Frontend uses this to redirect to change-password page
	private boolean firstLogin;
	
	private String profilePicture;
	private LocalDateTime createdAt;
}
