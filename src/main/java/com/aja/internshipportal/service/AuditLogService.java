package com.aja.internshipportal.service;

import com.aja.internshipportal.entity.AuditLog;
import com.aja.internshipportal.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogService {

    // Called internally from any service to record an action
    // User can be null for system-generated actions
    void log(User performedBy, String action,
             String entityType, Long entityId,
             String details, String ipAddress);

    // Admin → paginated audit log list
    Page<AuditLog> getAuditLogs(Pageable pageable);
}