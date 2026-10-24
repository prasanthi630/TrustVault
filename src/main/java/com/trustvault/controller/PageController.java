
package com.trustvault.controller;

import java.util.List;

import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import org.springframework.ui.Model;

import com.trustvault.repository.DocumentRepository;
import com.trustvault.repository.AuditLogRepository;

import com.trustvault.entity.AppUser;

import com.trustvault.service.AppUserService;
import com.trustvault.service.DocumentShareService;

import com.trustvault.model.Document;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@Controller
public class PageController {

    private final DocumentRepository documentRepository;

    private final AuditLogRepository auditLogRepository;

    private final AppUserService appUserService;

    private final DocumentShareService documentShareService;

    public PageController(
            DocumentRepository documentRepository,
            AuditLogRepository auditLogRepository,
            AppUserService appUserService,
            DocumentShareService documentShareService) {

        this.documentRepository = documentRepository;
        this.auditLogRepository = auditLogRepository;
        this.appUserService = appUserService;
        this.documentShareService = documentShareService;
    }

    // Home page
    @GetMapping("/")
    public String home() {

        return "home";
    }

    // Home page
    @GetMapping("/home")
    public String homePage() {

        return "home";
    }

    // Dashboard
    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        String username = userDetails.getUsername();

        // Get current user
        AppUser user =
                appUserService.findByEmail(username);

        // Get current user's documents
        List<Document> documents =
                documentRepository.findByOwnerEmail(username);

        // Get current user's audit logs
        var auditLogs =
                auditLogRepository.findByUsername(username);

        // User name
        model.addAttribute(
                "name",
                user.getName());

        // User's documents
        model.addAttribute(
                "documents",
                documents);

        // Total documents
        model.addAttribute(
                "totalDocuments",
                documents.size());

        // User's audit logs
        model.addAttribute(
                "auditLogs",
                auditLogs);

        // Total audit events
        model.addAttribute(
                "totalAuditEvents",
                auditLogs.size());

        // Documents shared by current user
        model.addAttribute(
                "sharedDocumentsCount",
                documentShareService
                        .getSharedByUser(username)
                        .size());

        return "dashboard";
    }

    // Login page
    @GetMapping("/login")
    public String login() {

        return "login";
    }

    // Registration page
    @GetMapping("/register")
    public String register() {

        return "register";
    }

    // Register user
    @PostMapping("/register")
    public String registerUser(
            @ModelAttribute AppUser user,
            Model model) {

        try {

            appUserService.registerUser(user);

            return "redirect:/login?registered";

        } catch (IllegalArgumentException exception) {

            model.addAttribute(
                    "error",
                    exception.getMessage());

            return "register";
        }
    }

    // Documents page
    @GetMapping("/documents")
    public String documents(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        String username = userDetails.getUsername();

        // Get only current user's documents
        List<Document> documents =
                documentRepository.findByOwnerEmail(username);

        model.addAttribute(
                "documents",
                documents);

        return "documents";
    }
}
