package com.aja.internshipportal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aja.internshipportal.entity.RefreshToken;
import com.aja.internshipportal.entity.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	
	 // Validate incoming refresh token from frontend
    Optional<RefreshToken> findByToken(String token);
    
    // Find existing token for user — to replace on new login
    Optional<RefreshToken> findByUser(User user);
    
    // Delete token on logout — clears session
    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.user = :user")
    void deleteByUser(@Param("user") User user);
}
