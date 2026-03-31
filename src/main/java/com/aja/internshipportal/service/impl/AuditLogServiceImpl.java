package com.aja.internshipportal.service.impl;

import com.aja.internshipportal.entity.AuditLog;
import com.aja.internshipportal.entity.User;
import com.aja.internshipportal.repository.AuditLogRepository;
import com.aja.internshipportal.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // ── Log an action ──
    // @Async — runs in background thread
    // never blocks the main request flow
    @Override
    @Async
    public void log(User performedBy, String action,
                    String entityType, Long entityId,
                    String details, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .performedBy(performedBy)
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .ipAddress(ipAddress)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit log saved — action: {} by: {}",
                action,
                performedBy != null ? performedBy.getEmail() : "system"
            );

        } catch (Exception e) {
            // never let audit log failure break the main flow
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }

    // ── Admin → get paginated logs ──
    @Override
    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}