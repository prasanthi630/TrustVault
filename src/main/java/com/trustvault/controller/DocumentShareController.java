
package com.trustvault.controller;

import com.trustvault.model.Document;
import com.trustvault.service.DocumentShareService;
import com.trustvault.service.AuditLogService;
import com.trustvault.service.WordToPdfService;
import com.trustvault.FileEncryptionUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import org.springframework.web.server.ResponseStatusException;

import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.logging.Level;
import java.util.logging.Logger;

@Controller
public class DocumentShareController {

    private static final Logger LOGGER =
            Logger.getLogger(DocumentShareController.class.getName());

    private final DocumentShareService documentShareService;
    private final AuditLogService auditLogService;
    private final WordToPdfService wordToPdfService;

    public DocumentShareController(
            DocumentShareService documentShareService,
            AuditLogService auditLogService,
            WordToPdfService wordToPdfService) {

        this.documentShareService = documentShareService;
        this.auditLogService = auditLogService;
        this.wordToPdfService = wordToPdfService;
    }

    // Shared Documents page
    @GetMapping("/shared-documents")
    public String sharedDocuments(
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        String username = userDetails.getUsername();

        model.addAttribute(
                "sharedDocuments",
                documentShareService.getSharedWithUser(username));

        model.addAttribute(
                "myShares",
                documentShareService.getSharedByUser(username));

        return "shared-documents";
    }

    // Share a document
    @PostMapping("/share-document")
    public String shareDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long documentId,
            @RequestParam String sharedWith,
            @RequestParam String permission,
            RedirectAttributes redirectAttributes) {

        String sharedBy = userDetails.getUsername();

        try {
            documentShareService.shareDocument(
                    documentId,
                    sharedBy,
                    sharedWith,
                    permission);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Document shared successfully with "
                            + sharedWith.trim() + "!");

        } catch (IllegalArgumentException | SecurityException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage() != null
                            ? exception.getMessage()
                            : "Unable to share document.");

        } catch (RuntimeException exception) {

            LOGGER.log(
                    Level.SEVERE,
                    "Document sharing failed for user: " + sharedBy,
                    exception);

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to share document. Please try again.");
        }

        return "redirect:/shared-documents";
    }

    // Revoke sharing permission
    @PostMapping("/revoke-share")
    public String revokeShare(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long shareId,
            RedirectAttributes redirectAttributes) {

        String username = userDetails.getUsername();

        try {
            documentShareService.revokeShare(
                    shareId,
                    username);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Sharing access revoked successfully!");

        } catch (IllegalArgumentException | SecurityException exception) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    exception.getMessage() != null
                            ? exception.getMessage()
                            : "Unable to revoke sharing access.");

        } catch (RuntimeException exception) {

            LOGGER.log(
                    Level.SEVERE,
                    "Share revocation failed for user: " + username,
                    exception);

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to revoke sharing access. Please try again.");
        }

        return "redirect:/shared-documents";
    }

    // Open shared document
    @GetMapping("/shared-document/{shareId}")
    public ResponseEntity<Resource> viewSharedDocument(
            @PathVariable Long shareId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            String username = userDetails.getUsername();

            // Verify recipient and VIEW permission
            Document document =
                    documentShareService.getSharedDocument(
                            shareId,
                            username);

            Path filePath = Path.of(document.getStoragePath());

            if (!Files.isRegularFile(filePath)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shared document file was not found.");
            }

            byte[] encryptedData = Files.readAllBytes(filePath);
            byte[] decryptedData =
                    FileEncryptionUtil.decrypt(encryptedData);

            String fileName = document.getFileName();

            if (fileName == null || fileName.isBlank()) {
                fileName = "document";
            }

            String lowerFileName =
                    fileName.toLowerCase(Locale.ROOT);

            String contentType = "application/octet-stream";

            // DOCX -> PDF conversion
            if (lowerFileName.endsWith(".docx")) {

                decryptedData =
                        wordToPdfService.convertDocxToPdf(decryptedData);

                fileName = fileName.substring(
                        0, fileName.length() - 5) + ".pdf";

                contentType = "application/pdf";
            }

            // Supported browser preview types
            else if (lowerFileName.endsWith(".pdf")) {
                contentType = "application/pdf";
            } else if (lowerFileName.endsWith(".jpg")
                    || lowerFileName.endsWith(".jpeg")) {
                contentType = "image/jpeg";
            } else if (lowerFileName.endsWith(".png")) {
                contentType = "image/png";
            } else if (lowerFileName.endsWith(".gif")) {
                contentType = "image/gif";
            } else if (lowerFileName.endsWith(".txt")) {
                contentType = "text/plain";
            } else {
                throw new ResponseStatusException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Preview is not supported for this file type.");
            }

            Resource resource = new ByteArrayResource(decryptedData);

            auditLogService.logAction(
                    username,
                    "VIEW",
                    document.getFileName());

            ContentDisposition disposition =
                    ContentDisposition.inline()
                            .filename(fileName, StandardCharsets.UTF_8)
                            .build();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            disposition.toString())
                    .body(resource);

        } catch (ResponseStatusException exception) {
            throw exception;

        } catch (IllegalArgumentException exception) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to access this document.");

        } catch (IOException exception) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to read shared document.");

        } catch (Exception exception) {

            LOGGER.log(Level.SEVERE, "Shared document preview failed.", exception);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to convert or open the shared document.");
        }
    }

    // Download shared document
    @GetMapping("/shared-document-download/{shareId}")
    public ResponseEntity<Resource> downloadSharedDocument(
            @PathVariable Long shareId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            String username = userDetails.getUsername();

            // Verify recipient and DOWNLOAD permission
            Document document =
                    documentShareService.getSharedDocumentForDownload(
                            shareId,
                            username);

            Path filePath = Path.of(document.getStoragePath());

            if (!Files.isRegularFile(filePath)) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Shared document file was not found.");
            }

            byte[] encryptedData = Files.readAllBytes(filePath);
            byte[] decryptedData =
                    FileEncryptionUtil.decrypt(encryptedData);

            String fileName = document.getFileName();

            if (fileName == null || fileName.isBlank()) {
                fileName = "document";
            }

            Resource resource = new ByteArrayResource(decryptedData);

            auditLogService.logAction(
                    username,
                    "DOWNLOAD",
                    fileName);

            ContentDisposition disposition =
                    ContentDisposition.attachment()
                            .filename(fileName, StandardCharsets.UTF_8)
                            .build();

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            disposition.toString())
                    .contentLength(decryptedData.length)
                    .body(resource);

        } catch (ResponseStatusException exception) {
            throw exception;

        } catch (IllegalArgumentException exception) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not authorized to download this document.");

        } catch (IOException exception) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to download shared document.");

        } catch (Exception exception) {

            LOGGER.log(
                    Level.SEVERE,
                    "Shared document download failed.",
                    exception);

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to process shared document download.");
        }
    }
}
