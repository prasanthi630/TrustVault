
package com.trustvault.service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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

    private static final Logger LOGGER =
            Logger.getLogger(AccountDeletionService.class.getName());

    private final AppUserRepository appUserRepository;
    private final DocumentRepository documentRepository;
    private final DocumentShareRepository documentShareRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    private final Path uploadDirectory =
            Paths.get("uploads").toAbsolutePath().normalize();

    public AccountDeletionService(
            AppUserRepository appUserRepository,
            DocumentRepository documentRepository,
            DocumentShareRepository documentShareRepository,
            AuditLogRepository auditLogRepository,
            PasswordEncoder passwordEncoder,
            PlatformTransactionManager transactionManager) {

        this.appUserRepository = appUserRepository;
        this.documentRepository = documentRepository;
        this.documentShareRepository = documentShareRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
    }

    public void deleteAccount(String email, String currentPassword)
            throws IOException {

        // Find the account belonging to the logged-in user.
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        // Verify the current password before changing anything.
        if (currentPassword == null
                || currentPassword.isBlank()
                || !passwordEncoder.matches(
                        currentPassword, user.getPassword())) {

            throw new IllegalArgumentException(
                    "Incorrect password. Account was not deleted.");
        }

        // Collect only documents owned by this user.
        List<Document> ownedDocuments =
                documentRepository.findByOwnerEmail(email);

        Map<Long, String> expectedDocuments = new HashMap<>();

        for (Document document : ownedDocuments) {
            expectedDocuments.put(
                    document.getId(), document.getStoragePath());
        }

        // Track files moved to temporary storage.
        Map<Path, Path> stagedFiles = new LinkedHashMap<>();

        Path stagingDirectory = null;
        Path stagingPath = null;

        try {
            // Move encrypted files into temporary storage.
            for (Document document : ownedDocuments) {

                String storagePath = document.getStoragePath();

                if (storagePath == null || storagePath.isBlank()) {
                    throw new IOException(
                            "A document storage path is missing. "
                            + "Account was not deleted.");
                }

                Path originalPath = Paths.get(storagePath)
                        .toAbsolutePath()
                        .normalize();

                // Permit only files directly inside uploads.
                if (originalPath.getParent() == null
                        || !originalPath.getParent()
                                .equals(uploadDirectory)) {

                    throw new IOException(
                            "A document is outside the expected uploads "
                            + "directory. Account was not deleted.");
                }

                // A missing file is logged, and its database record
                // can still be cleaned up.
                if (!Files.exists(originalPath)) {
                    LOGGER.warning(
                            "Stored document file is already missing: "
                            + originalPath);
                    continue;
                }

                if (!Files.isRegularFile(originalPath)) {
                    throw new IOException(
                            "A stored document path is not a regular file. "
                            + "Account was not deleted.");
                }

                if (stagingDirectory == null) {
                    stagingDirectory = uploadDirectory.resolve(
                            ".trustvault-delete-" + UUID.randomUUID());

                    Files.createDirectory(stagingDirectory);
                }

                Path temporaryPath = stagingDirectory.resolve(
                        UUID.randomUUID().toString());

                moveFile(originalPath, temporaryPath);

                stagedFiles.put(originalPath, temporaryPath);
            }

            // FIX: Keep stagingPath in the outer scope so it remains
            // available after the try block.
            stagingPath = stagingDirectory;

            // Commit all database changes in one transaction.
            transactionTemplate.executeWithoutResult(transactionStatus -> {

                AppUser currentUser = appUserRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User not found."));

                // Check password again before deleting.
                if (!passwordEncoder.matches(
                        currentPassword, currentUser.getPassword())) {

                    throw new IllegalArgumentException(
                            "Password verification failed.");
                }

                List<Document> currentDocuments =
                        documentRepository.findByOwnerEmail(email);

                Map<Long, String> currentDocumentMap = new HashMap<>();

                for (Document document : currentDocuments) {
                    currentDocumentMap.put(
                            document.getId(), document.getStoragePath());
                }

                if (!expectedDocuments.equals(currentDocumentMap)) {
                    throw new IllegalStateException(
                            "Documents changed during deletion. "
                            + "Please retry.");
                }

                List<Long> documentIds = currentDocuments.stream()
                        .map(Document::getId)
                        .toList();

                // Gather all permissions created by or granted to the user.
                // Also gather shares belonging to the user's documents.
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

                documentShareRepository.deleteAll(
                        sharesToDelete.values());

                // Encrypted files are staged, so remove their DB records.
                documentRepository.deleteAll(currentDocuments);

                // Preserve historical audit records anonymously.
                List<AuditLog> auditLogs =
                        auditLogRepository.findByUsername(email);

                for (AuditLog log : auditLogs) {
                    log.setUsername("Deleted User");
                }

                auditLogRepository.saveAll(auditLogs);

                // Delete the account last.
                appUserRepository.delete(currentUser);
            });

        } catch (RuntimeException | IOException exception) {

            // If staging or the database transaction fails,
            // attempt to restore every file to its original location.
            try {
                restoreFiles(stagedFiles);
                removeEmptyStagingDirectory(stagingDirectory);

            } catch (IOException restoreException) {
                exception.addSuppressed(restoreException);

                LOGGER.log(
                        Level.SEVERE,
                        "Some staged files could not be restored. "
                        + "Check the uploads directory immediately.",
                        restoreException);
            }

            throw exception;
        }

        // Database transaction committed successfully.
        // Permanently remove staged encrypted files.
        cleanupStagedFiles(stagedFiles, stagingPath);
    }

    private void moveFile(Path source, Path destination)
            throws IOException {

        try {
            Files.move(
                    source,
                    destination,
                    StandardCopyOption.ATOMIC_MOVE);

        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination);
        }
    }

    private void restoreFiles(Map<Path, Path> stagedFiles)
            throws IOException {

        List<Map.Entry<Path, Path>> entries =
                new ArrayList<>(stagedFiles.entrySet());

        Collections.reverse(entries);

        IOException failure = null;

        for (Map.Entry<Path, Path> entry : entries) {

            Path originalPath = entry.getKey();
            Path temporaryPath = entry.getValue();

            try {
                if (Files.exists(temporaryPath)) {

                    if (Files.exists(originalPath)) {
                        throw new IOException(
                                "Cannot restore file because the original "
                                + "path already exists: " + originalPath);
                    }

                    moveFile(temporaryPath, originalPath);
                }

            } catch (IOException exception) {

                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }

        if (failure != null) {
            throw failure;
        }
    }

    private void removeEmptyStagingDirectory(Path directory)
            throws IOException {

        if (directory != null && Files.isDirectory(directory)) {
            Files.deleteIfExists(directory);
        }
    }

    private void cleanupStagedFiles(
            Map<Path, Path> stagedFiles,
            Path stagingDirectory) {

        for (Map.Entry<Path, Path> entry : stagedFiles.entrySet()) {

            Path temporaryPath = entry.getValue();

            try {
                Files.deleteIfExists(temporaryPath);

            } catch (IOException exception) {
                LOGGER.log(
                        Level.SEVERE,
                        "Account was deleted, but a staged encrypted "
                        + "file could not be removed: " + temporaryPath,
                        exception);
            }
        }

        try {
            removeEmptyStagingDirectory(stagingDirectory);

        } catch (IOException exception) {
            LOGGER.log(
                    Level.WARNING,
                    "Temporary document cleanup is incomplete.",
                    exception);
        }
    }
}
