
package com.trustvault.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String action;

    private String documentName;

    private LocalDateTime timestamp;

    public AuditLog() {
    }

    public AuditLog(String username, String action,
                   String documentName) {
        this.username = username;
        this.action = action;
        this.documentName = documentName;
        this.timestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getAction() {
        return action;
    }

    public String getDocumentName() {
        return documentName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
