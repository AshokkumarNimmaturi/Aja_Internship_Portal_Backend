package com.aja.internshipportal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aja.internshipportal.entity.PasswordResetToken;
import com.aja.internshipportal.entity.User;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

	 // Validate token from reset email link
    Optional<PasswordResetToken> findByToken(String token);
    
    // Clean up old tokens for user before issuing new one
    @Modifying
    @Query("DELETE FROM PasswordResetToken p WHERE p.user = :user")
    void deleteByUser(@Param("user") User user);
}
