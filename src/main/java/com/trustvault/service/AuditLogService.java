
package com.trustvault.service;

import org.springframework.stereotype.Service;

import com.trustvault.entity.AuditLog;
import com.trustvault.repository.AuditLogRepository;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void logAction(String username, String action,
                         String documentName) {

        AuditLog auditLog = new AuditLog(
                username,
                action,
                documentName
        );

        auditLogRepository.save(auditLog);
    }
}
