
package com.trustvault.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import org.springframework.http.ContentDisposition;
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
                    file, getUsername());

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
                    "Unable to upload document. Please try again.");
        }

        return "redirect:/documents";
    }

    // Delete document
    @PostMapping("/documents/delete/{id}")
    public String deleteDocument(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        try {
            Document document = documentService.getDocument(
                    id, getUsername());

            String documentName = document.getFileName();

            documentService.deleteDocument(id, getUsername());

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
                    "Unable to delete document. Please try again.");
        }

        return "redirect:/documents";
    }

    // Download original document after decryption
    @GetMapping("/documents/download/{id}")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable Long id) {

        return serveDocument(id, false);
    }

    // View PDF or image in the browser
    @GetMapping("/documents/view/{id}")
    public ResponseEntity<Resource> viewDocument(
            @PathVariable Long id) {

        return serveDocument(id, true);
    }

    // Shared file-serving logic for view and download
    private ResponseEntity<Resource> serveDocument(
            Long id, boolean inline) {

        String username = getUsername();
        Document document;

        // Check ownership before reading the file
        try {
            document = documentService.getDocument(id, username);

        } catch (SecurityException exception) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to access this document.");

        } catch (IllegalArgumentException exception) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Document not found.");
        }

        String contentType = document.getContentType();

        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        MediaType mediaType;

        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (IllegalArgumentException exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        // Only allow safe document types for browser preview
        if (inline) {
            String fileName = document.getFileName() == null
                    ? ""
                    : document.getFileName().toLowerCase();

            boolean previewable =
                    fileName.endsWith(".pdf")
                    || fileName.endsWith(".png")
                    || fileName.endsWith(".jpg")
                    || fileName.endsWith(".jpeg");

            if (!previewable) {
                throw new ResponseStatusException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Preview is available for PDF and image files only.");
            }

            // Use a controlled media type rather than trusting uploaded
            // content-type metadata.
            if (fileName.endsWith(".pdf")) {
                mediaType = MediaType.APPLICATION_PDF;
            } else if (fileName.endsWith(".png")) {
                mediaType = MediaType.IMAGE_PNG;
            } else {
                mediaType = MediaType.IMAGE_JPEG;
            }
        }

        Path filePath = Path.of(document.getStoragePath());

        if (!Files.isRegularFile(filePath)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Stored document file was not found.");
        }

        byte[] decryptedData;

        try {
            byte[] encryptedData = Files.readAllBytes(filePath);
            decryptedData = FileEncryptionUtil.decrypt(encryptedData);

        } catch (IOException | RuntimeException exception) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to read or decrypt the document.");
        }

        // Record activity only after the file has been read and decrypted
        auditLogService.logAction(
                username,
                inline ? "VIEW" : "DOWNLOAD",
                document.getFileName());

        Resource resource = new ByteArrayResource(decryptedData);

        String fileName = document.getFileName() == null
                ? "document"
                : document.getFileName();

        ContentDisposition disposition =
                ContentDisposition.builder(inline ? "inline" : "attachment")
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString())
                .body(resource);
    }
}
