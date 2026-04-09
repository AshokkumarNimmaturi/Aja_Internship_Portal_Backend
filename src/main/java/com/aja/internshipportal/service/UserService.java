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

	// ✅ ADDED: Allow any logged-in user to update their own profile tech interests
	UserResponse updateMyProfile(String email, UpdateUserRequest request);

	// Admin → activate user (re-enable access)
    void activateUser(Long id);

    // ✅ NEW: Support Center Methods
    
    // Toggle whether the agent is "Available" to receive calls
    UserResponse toggleAvailability(String email);

    // Update real-time "Busy" status based on Twilio call events
    void setInCallStatus(String phone, boolean inCall);
}
