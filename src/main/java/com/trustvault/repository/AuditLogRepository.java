
package com.trustvault.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trustvault.entity.AuditLog;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUsername(String username);

}
