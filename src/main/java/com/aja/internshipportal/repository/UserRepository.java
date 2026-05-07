package com.aja.internshipportal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aja.internshipportal.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByPhone(String phone);
    List<User> findByRole(User.Role role);
    List<User> findByEnabled(boolean enabled);
    List<User> findByRoleAndEnabled(User.Role role, boolean enabled);

    // ✅ Finds agents who are READY for calls (AVAILABLE + NOT BUSY)
    @Query("SELECT u FROM User u WHERE u.role IN ('ADMIN', 'TUTOR') " +
           "AND u.status = 'AVAILABLE' " +
           "AND u.inCall = false " +
           "AND u.enabled = true")
    List<User> findAvailableSupportAgents();

    // ✅ Counts any agents who are signed in (AVAILABLE or on BREAK)
    @Query("SELECT COUNT(u) FROM User u WHERE u.role IN ('ADMIN', 'TUTOR') " +
           "AND u.status != 'OFFLINE' " +
           "AND u.enabled = true")
    long countOnlineAgents();
    
    // ✅ NEW: Robust phone matching for real-time status updates
    // Finds a user whose phone number ends with the given 10 digits
    @Query("SELECT u FROM User u WHERE u.phone LIKE %:phoneSuffix")
    Optional<User> findByPhoneEndingWith(@Param("phoneSuffix") String phoneSuffix);

    List<User> findByRoleInAndStatusAndInCallFalseAndEnabledTrue(List<User.Role> roles, User.SupportStatus status);
}
