
package com.trustvault.controller;

import com.trustvault.repository.AuditLogRepository;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(
            AuditLogRepository auditLogRepository) {

        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/audit-trail")
    public String auditTrail(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        String username = userDetails.getUsername();

        model.addAttribute(
                "auditLogs",
                auditLogRepository.findByUsername(username)
        );

        return "audit-trail";
    }
}
