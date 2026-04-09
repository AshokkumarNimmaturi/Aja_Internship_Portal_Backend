package com.aja.internshipportal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.aja.internshipportal.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	//Used During Login - find user by email
	Optional<User> findByEmail(String email);
	
	// Check if email already registered — used during register
    boolean existsByEmail(String email);
    
    // Check if phone already registered — used during user creation
    boolean existsByPhone(String phone);
    
    // Admin → user list filtered by role
    List<User> findByRole(User.Role role);
    
    // Admin → list only active or deactivated users
    List<User> findByEnabled(boolean enabled);
    
    // Admin → filter by role AND status together
    List<User> findByRoleAndEnabled(User.Role role, boolean enabled);

    // ✅ NEW: Find available agents for the support queue
    // Looks for specific roles (Admin/Tutor) who are marked 'available' and are not 'inCall'
    List<User> findByRoleInAndAvailableTrueAndInCallFalseAndEnabledTrue(List<User.Role> roles);
    
}
