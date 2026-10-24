
package com.trustvault.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import com.trustvault.FileEncryptionUtil;
import com.trustvault.model.Document;
import com.trustvault.repository.DocumentRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    private final Path uploadDirectory =
            Paths.get("uploads").toAbsolutePath().normalize();

    public DocumentService(
            DocumentRepository documentRepository) {

        this.documentRepository = documentRepository;
    }

    // Upload document
    public Document uploadFile(
            MultipartFile file,
            String ownerEmail) throws IOException {

        // Check empty file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select a file.");
        }

        // Check file size
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "File size must be 10 MB or less.");
        }

        // Get original file name
        String originalName = Paths.get(
                file.getOriginalFilename() == null
                        ? "document"
                        : file.getOriginalFilename()
        ).getFileName().toString();

        // Get file extension
        String extension = "";

        int dotIndex = originalName.lastIndexOf('.');

        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex)
                    .toLowerCase();
        }

        // Allowed file types
        List<String> allowedExtensions = List.of(
                ".pdf",
                ".png",
                ".jpg",
                ".jpeg",
                ".doc",
                ".docx"
        );

        // Validate file extension
        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException(
                    "Unsupported file type. Please upload a supported document.");
        }

        // Read file data only once
        byte[] fileData = file.getBytes();

        // Validate actual PDF content
        if (extension.equals(".pdf")) {

            if (fileData.length < 5
                    || !new String(
                            fileData,
                            0,
                            5,
                            java.nio.charset.StandardCharsets.US_ASCII
                    ).equals("%PDF-")) {

                throw new IllegalArgumentException(
                        "Invalid PDF file.");
            }
        }

        // Create upload directory
        Files.createDirectories(uploadDirectory);

        // Generate unique file name
        String storedName = UUID.randomUUID() + extension;

        Path destination = uploadDirectory
                .resolve(storedName)
                .normalize();

        // Check file path
        if (!destination.startsWith(uploadDirectory)) {
            throw new SecurityException(
                    "Invalid file path.");
        }

        // Encrypt file
        byte[] encryptedData =
                FileEncryptionUtil.encrypt(fileData);

        // Save encrypted file
        Files.write(destination, encryptedData);

        // Create document object
        Document document = new Document();

        document.setFileName(originalName);
        document.setContentType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setStoragePath(destination.toString());

        // Save document owner
        document.setOwnerEmail(ownerEmail);

        try {

            // Save document in database
            return documentRepository.save(document);

        } catch (RuntimeException exception) {

            // Delete encrypted file if database save fails
            Files.deleteIfExists(destination);

            throw exception;
        }
    }

    // Get document only if it belongs to the logged-in user
    public Document getDocument(
            Long id,
            String ownerEmail) {

        Document document = documentRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Document not found."));

        // Check document ownership
        if (document.getOwnerEmail() == null
                || !document.getOwnerEmail()
                        .equalsIgnoreCase(ownerEmail)) {

            throw new SecurityException(
                    "You are not authorized to access this document.");
        }

        return document;
    }

    // Delete document only if it belongs to the logged-in user
    public void deleteDocument(
            Long id,
            String ownerEmail) throws IOException {

        // Check document ownership
        Document document = getDocument(id, ownerEmail);

        // Get file path
        Path filePath =
                Path.of(document.getStoragePath());

        // Delete encrypted file
        Files.deleteIfExists(filePath);

        // Delete database record
        documentRepository.delete(document);
    }
}
