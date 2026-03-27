package com.aja.internshipportal.service;

import java.util.List;

import com.aja.internshipportal.dto.request.CreateUserRequest;
import com.aja.internshipportal.dto.request.UpdateUserRequest;
import com.aja.internshipportal.dto.response.UserResponse;

public interface UserService {

	// Admin -> create internal user (TUTOR OR EMPLOYEE)
	UserResponse createuser(CreateUserRequest request);

	// Admin → list all users
	List<UserResponse> getAllUsers();

	// Admin → update role or status
	UserResponse updateUser(Long id, UpdateUserRequest request);

	// Admin → deactivate user (soft delete)
	void deactivateUser(Long id);

	// Any logged-in user → get own profile
	UserResponse getMyProfile(String email);

}
