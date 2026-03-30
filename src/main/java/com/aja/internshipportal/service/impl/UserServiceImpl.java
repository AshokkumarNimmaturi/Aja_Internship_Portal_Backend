package com.aja.internshipportal.service.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aja.internshipportal.dto.request.CreateUserRequest;
import com.aja.internshipportal.dto.request.UpdateUserRequest;
import com.aja.internshipportal.dto.response.UserResponse;
import com.aja.internshipportal.entity.User;
import com.aja.internshipportal.exception.AppException;
import com.aja.internshipportal.repository.UserRepository;
import com.aja.internshipportal.service.EmailService;
import com.aja.internshipportal.service.PdfService;
import com.aja.internshipportal.service.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	// remove these two commented lines
	// private final EmailService emailService;
	// private final PdfService pdfService;

	// add these properly
	private final EmailService emailService;
	private final PdfService pdfService;

	@Override
	public UserResponse createuser(CreateUserRequest request) {
		// check duplicate email
		if (userRepository.existsByEmail(request.getEmail())) {
			throw AppException.conflict("Email already registered with our database");
		}

		// check duplicate phone
		if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
			throw AppException.conflict("Phone number already registered");
		}

		// only TUTOR or EMPLOYEE can be created by admin
		// ADMIN cannot be created via this endpoint

		if (request.getRole() == User.Role.ADMIN || request.getRole() == User.Role.SUBSCRIBER) {
			throw AppException.badRequest("Only TUTOR or EMPLOYEE accounts can be created here");
		}

		// generate random temporary password
		// user will be forced to change this on first login

		String tempPassword = generateTempPassword();
		User user = User.builder().fullName(request.getFullName()).email(request.getEmail())
				.password(passwordEncoder.encode(tempPassword)).phone(request.getPhone()).role(request.getRole())
				.enabled(true).firstLogin(true) // forces password change on first login
				.build();

		userRepository.save(user);

		// TODO — generate PDF with credentials and send email
		// byte[] pdf = pdfService.generateCredentialsPdf(user, tempPassword);
		// emailService.sendCredentialsEmail(user.getEmail(), user.getFullName(), pdf);
		
		// remove this
		// TODO — generate PDF with credentials and send email
		// byte[] pdf = pdfService.generateCredentialsPdf(user, tempPassword);
		// emailService.sendCredentialsEmail(user.getEmail(), user.getFullName(), pdf);

		// add this
		byte[] pdf = pdfService.generateCredentialsPdf(user, tempPassword);
		emailService.sendCredentialsEmail(
		    user.getEmail(),
		    user.getFullName(),
		    tempPassword,
		    pdf
		);

		return mapToUserResponse(user);
	}

	// ── Admin → list all users ──
	@Override
	public List<UserResponse> getAllUsers() {
		return userRepository.findAll().stream().map(this::mapToUserResponse).collect(Collectors.toList());
	}

	// ── Admin → update role or enabled status ──
	@Override
	@Transactional
	public UserResponse updateUser(Long id, UpdateUserRequest request) {

		User user = userRepository.findById(id).orElseThrow(() -> AppException.notFound("User not found"));

		// update only fields that are provided
		if (request.getFullName() != null) {
			user.setFullName(request.getFullName());
		}

		if (request.getRole() != null) {
			user.setRole(request.getRole());
		}

		if (request.getEnabled() != null) {
			user.setEnabled(request.getEnabled());
		}

		userRepository.save(user);
		return mapToUserResponse(user);

	}

	// ── Admin → deactivate user (soft delete) ──
	// we never delete users from DB — just disable them
	@Override
	@Transactional
	public void deactivateUser(Long id) {

		User user = userRepository.findById(id).orElseThrow(() -> AppException.notFound("User not found"));

		user.setEnabled(false);
		userRepository.save(user);
	}

	// ── Any user → get own profile ──
	@Override
	public UserResponse getMyProfile(String email) {
		User user = userRepository.findByEmail(email).orElseThrow(() -> AppException.notFound("User not found"));
		return mapToUserResponse(user);
	}

	// helpers

	// generates a random 8 - character temp password
	// e.g "jfhgkjgHJKi43"
	private String generateTempPassword() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
	}

	public UserResponse mapToUserResponse(User user) {
		return UserResponse.builder().id(user.getId()).fullName(user.getFullName()).email(user.getEmail())
				.phone(user.getPhone()).role(user.getRole()).enabled(user.isEnabled()).firstLogin(user.isFirstLogin())
				.profilePicture(user.getProfilePicture()).createdAt(user.getCreatedAt()).build();
	}

}
