
package com.trustvault.service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trustvault.entity.AppUser;
import com.trustvault.entity.AuditLog;
import com.trustvault.entity.DocumentShare;
import com.trustvault.model.Document;
import com.trustvault.repository.AppUserRepository;
import com.trustvault.repository.AuditLogRepository;
import com.trustvault.repository.DocumentRepository;
import com.trustvault.repository.DocumentShareRepository;

@Service
public class AccountDeletionService {

    private final AppUserRepository appUserRepository;
    private final DocumentRepository documentRepository;
    private final DocumentShareRepository documentShareRepository;
    private final AuditLogRepository auditLogRepository;
    private final DocumentService documentService;
    private final PasswordEncoder passwordEncoder;

    public AccountDeletionService(
            AppUserRepository appUserRepository,
            DocumentRepository documentRepository,
            DocumentShareRepository documentShareRepository,
            AuditLogRepository auditLogRepository,
            DocumentService documentService,
            PasswordEncoder passwordEncoder) {

        this.appUserRepository = appUserRepository;
        this.documentRepository = documentRepository;
        this.documentShareRepository = documentShareRepository;
        this.auditLogRepository = auditLogRepository;
        this.documentService = documentService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void deleteAccount(String email, String currentPassword)
            throws IOException {

        // Find the account belonging to the logged-in user
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        // Require the correct password
        if (currentPassword == null
                || currentPassword.isBlank()
                || !passwordEncoder.matches(
                        currentPassword, user.getPassword())) {

            throw new IllegalArgumentException(
                    "Incorrect password. Account was not deleted.");
        }

        // Get only this user's documents
        List<Document> ownedDocuments =
                documentRepository.findByOwnerEmail(email);

        List<Long> documentIds = ownedDocuments.stream()
                .map(Document::getId)
                .toList();

        // Collect shares created by or received by this user
        // and shares attached to documents owned by this user.
        Map<Long, DocumentShare> sharesToDelete =
                new LinkedHashMap<>();

        for (DocumentShare share :
                documentShareRepository.findBySharedBy(email)) {
            sharesToDelete.put(share.getId(), share);
        }

        for (DocumentShare share :
                documentShareRepository.findBySharedWith(email)) {
            sharesToDelete.put(share.getId(), share);
        }

        if (!documentIds.isEmpty()) {
            for (DocumentShare share :
                    documentShareRepository
                            .findByDocumentIdIn(documentIds)) {
                sharesToDelete.put(share.getId(), share);
            }
        }

        // Remove sharing permissions related to this account.
        documentShareRepository.deleteAll(
                sharesToDelete.values());

        // Delete the user's own encrypted files and database records.
        // Existing ownership checks in DocumentService are retained.
        for (Document document : ownedDocuments) {
            documentService.deleteDocument(
                    document.getId(), email);
        }

        // Preserve historical audit events without retaining this email.
        List<AuditLog> auditLogs =
                auditLogRepository.findByUsername(email);

        for (AuditLog log : auditLogs) {
            log.setUsername("Deleted User");
        }

        auditLogRepository.saveAll(auditLogs);

        // Delete the account only after the preceding steps succeed.
        appUserRepository.delete(user);
    }
}
