package com.aja.internshipportal.repository;

import org.springframework.data.domain.Pageable; // ADD THIS
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import com.aja.internshipportal.entity.AuditLog;
import com.aja.internshipportal.entity.User;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

	   // Admin audit log page — paginated, newest first
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // Filter by action type e.g. "USER_CREATED"
    List<AuditLog> findByAction(String action);
    
 // Filter logs by a specific user
    List<AuditLog> findByPerformedBy(User user);
    
    // Filter logs within a date range — admin date filter
    List<AuditLog> findByCreatedAtBetween(
            LocalDateTime from,
            LocalDateTime to
    );
	
}
