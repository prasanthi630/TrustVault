
package com.trustvault.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.springframework.web.server.ResponseStatusException;

import org.springframework.security.core.context.SecurityContextHolder;

import com.trustvault.FileEncryptionUtil;
import com.trustvault.model.Document;
import com.trustvault.service.DocumentService;
import com.trustvault.service.AuditLogService;

@Controller
public class DocumentController {

    private final DocumentService documentService;

    private final AuditLogService auditLogService;

    public DocumentController(
            DocumentService documentService,
            AuditLogService auditLogService) {

        this.documentService = documentService;
        this.auditLogService = auditLogService;
    }

    // Get logged-in username
    private String getUsername() {

        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }

    // Upload document
    @PostMapping("/documents/upload")
    public String uploadDocument(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        try {

            Document document = documentService.uploadFile(
                    file,
                    getUsername());

            auditLogService.logAction(
                    getUsername(),
                    "UPLOAD",
                    document.getFileName());

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Document uploaded successfully!");

        } catch (IllegalArgumentException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage());

        } catch (IOException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to upload document.");
        }

        return "redirect:/documents";
    }

    // Delete document
    @PostMapping("/documents/delete/{id}")
    public String deleteDocument(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {

            // Check document ownership
            Document document = documentService.getDocument(
                    id,
                    getUsername());

            String documentName = document.getFileName();

            // Delete document
            documentService.deleteDocument(
                    id,
                    getUsername());

            // Save audit log
            auditLogService.logAction(
                    getUsername(),
                    "DELETE",
                    documentName);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Document deleted successfully!");

        } catch (SecurityException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "You are not authorized to delete this document.");

        } catch (IllegalArgumentException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Document not found.");

        } catch (IOException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to delete document.");
        }

        return "redirect:/documents";
    }

    // Download document
    @GetMapping("/documents/download/{id}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id) {

        try {

            // Check document ownership
            Document document = documentService.getDocument(
                    id,
                    getUsername());

            // Get encrypted file path
            Path filePath =
                    Path.of(document.getStoragePath());

            // Read encrypted file
            byte[] encryptedData =
                    Files.readAllBytes(filePath);

            // Decrypt file
            byte[] decryptedData =
                    FileEncryptionUtil.decrypt(encryptedData);

            // Prepare downloadable resource
            Resource resource =
                    new ByteArrayResource(decryptedData);

            String contentType = document.getContentType();

            if (contentType == null
                    || contentType.isBlank()) {

                contentType = "application/octet-stream";
            }

            // Save audit log
            auditLogService.logAction(
                    getUsername(),
                    "DOWNLOAD",
                    document.getFileName());

            return ResponseEntity.ok()
                    .contentType(
                            MediaType.parseMediaType(contentType))
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\""
                                    + document.getFileName()
                                    + "\"")
                    .body(resource);

        } catch (SecurityException exception) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to access this document.");

        } catch (IllegalArgumentException exception) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Document not found.");

        } catch (IOException exception) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to download file.");
        }
    }
}
