package com.aja.internshipportal.service;

import java.util.List;

import com.aja.internshipportal.dto.request.CreateUserRequest;
import com.aja.internshipportal.dto.request.UpdateUserRequest;
import com.aja.internshipportal.dto.response.UserResponse;
import com.aja.internshipportal.entity.User;

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

    // ✅ UPDATED: Support Center Methods
    
    // Set the agent's current status (AVAILABLE, BREAK, OFFLINE)
    UserResponse updateSupportStatus(String email, User.SupportStatus status);

    // Update real-time "Busy" status based on Twilio call events
    void setInCallStatus(String phone, boolean inCall);
    
    // ✅ UPDATED: Now uses SID for perfect matching
    void markCallAsAnswered(String agentPhone, String callSid);
}
