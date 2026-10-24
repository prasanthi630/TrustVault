
package com.trustvault.controller;

import com.trustvault.model.Document;
import com.trustvault.service.DocumentShareService;
import com.trustvault.service.AuditLogService;
import com.trustvault.service.WordToPdfService;
import com.trustvault.FileEncryptionUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

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

@Controller
public class DocumentShareController {

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
                documentShareService.getSharedWithUser(username)
        );

        model.addAttribute(
                "myShares",
                documentShareService.getSharedByUser(username)
        );

        return "shared-documents";
    }

    // Share a document
    @PostMapping("/share-document")
    public String shareDocument(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long documentId,
            @RequestParam String sharedWith,
            @RequestParam String permission) {

        String sharedBy = userDetails.getUsername();

        documentShareService.shareDocument(
                documentId,
                sharedBy,
                sharedWith,
                permission
        );

        return "redirect:/shared-documents";
    }

    // Revoke sharing permission
    @PostMapping("/revoke-share")
    public String revokeShare(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long shareId) {

        String username = userDetails.getUsername();

        documentShareService.revokeShare(
                shareId,
                username
        );

        return "redirect:/shared-documents";
    }

    // Open shared document
    @GetMapping("/shared-document/{shareId}")
    public ResponseEntity<Resource> viewSharedDocument(
            @PathVariable Long shareId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {

            String username = userDetails.getUsername();

            // Verify recipient and VIEW or DOWNLOAD permission
            Document document =
                    documentShareService.getSharedDocument(
                            shareId,
                            username
                    );

            // Read encrypted file
            Path filePath = Path.of(document.getStoragePath());

            byte[] encryptedData =
                    Files.readAllBytes(filePath);

            // Decrypt file
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
                        wordToPdfService.convertDocxToPdf(
                                decryptedData
                        );

                fileName = fileName.substring(
                        0,
                        fileName.length() - 5
                ) + ".pdf";

                contentType = "application/pdf";
            }

            // PDF files
            else if (lowerFileName.endsWith(".pdf")) {

                contentType = "application/pdf";
            }

            // JPG images
            else if (lowerFileName.endsWith(".jpg")
                    || lowerFileName.endsWith(".jpeg")) {

                contentType = "image/jpeg";
            }

            // PNG images
            else if (lowerFileName.endsWith(".png")) {

                contentType = "image/png";
            }

            // GIF images
            else if (lowerFileName.endsWith(".gif")) {

                contentType = "image/gif";
            }

            // TXT files
            else if (lowerFileName.endsWith(".txt")) {

                contentType = "text/plain";
            }

            // Unsupported preview file type
            else {

                throw new ResponseStatusException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Preview is not supported for this file type."
                );
            }

            Resource resource =
                    new ByteArrayResource(decryptedData);

            // Audit Trail
            auditLogService.logAction(
                    username,
                    "VIEW",
                    document.getFileName()
            );

            // Open document in browser
            return ResponseEntity.ok()
                    .contentType(
                            MediaType.parseMediaType(contentType)
                    )
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + fileName + "\""
                    )
                    .body(resource);

        } catch (ResponseStatusException exception) {

            throw exception;

        } catch (IllegalArgumentException exception) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    exception.getMessage()
            );

        } catch (IOException exception) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to read shared document."
            );

        } catch (Exception exception) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to convert or open the shared document."
            );
        }
    }

    // Download shared document
    @GetMapping("/shared-document-download/{shareId}")
    public ResponseEntity<Resource> downloadSharedDocument(
            @PathVariable Long shareId,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {

            String username = userDetails.getUsername();

            // Verify recipient and DOWNLOAD permission only
            Document document =
                    documentShareService.getSharedDocumentForDownload(
                            shareId,
                            username
                    );

            // Read encrypted file
            Path filePath = Path.of(document.getStoragePath());

            byte[] encryptedData =
                    Files.readAllBytes(filePath);

            // Decrypt file
            byte[] decryptedData =
                    FileEncryptionUtil.decrypt(encryptedData);

            // Original file name
            String fileName = document.getFileName();

            if (fileName == null || fileName.isBlank()) {
                fileName = "document";
            }

            // Return decrypted document as downloadable attachment
            Resource resource =
                    new ByteArrayResource(decryptedData);

            // Audit Trail
            auditLogService.logAction(
                    username,
                    "DOWNLOAD",
                    fileName
            );

            // Download response
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\""
                    )
                    .contentLength(decryptedData.length)
                    .body(resource);

        } catch (ResponseStatusException exception) {

            throw exception;

        } catch (IllegalArgumentException exception) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    exception.getMessage()
            );

        } catch (IOException exception) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to download shared document."
            );

        } catch (Exception exception) {

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to process shared document download."
            );
        }
    }
}
