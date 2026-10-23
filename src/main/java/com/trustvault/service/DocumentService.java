
package com.trustvault.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import com.trustvault.FileEncryptionUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.trustvault.model.Document;
import com.trustvault.repository.DocumentRepository;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;

    private final Path uploadDirectory =
            Paths.get("uploads").toAbsolutePath().normalize();

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    // Upload document
    public Document uploadFile(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Please select a file.");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "File size must be 10 MB or less.");
        }

        String originalName = Path.of(
                file.getOriginalFilename() == null
                        ? "document"
                        : file.getOriginalFilename()
        ).getFileName().toString();

        String extension = "";

        int dotIndex = originalName.lastIndexOf('.');

        if (dotIndex >= 0) {
            extension = originalName.substring(dotIndex)
                    .toLowerCase();
        }

        List<String> allowedExtensions = List.of(
                ".pdf", ".png", ".jpg", ".jpeg",
                ".doc", ".docx", ".txt"
        );

        if (!allowedExtensions.contains(extension)) {
            throw new IllegalArgumentException(
                    "Unsupported file type.");
        }

        Files.createDirectories(uploadDirectory);

        String storedName = UUID.randomUUID() + extension;

        Path destination = uploadDirectory
                .resolve(storedName)
                .normalize();

        if (!destination.startsWith(uploadDirectory)) {
            throw new SecurityException(
                    "Invalid file path.");
        }

        byte[] fileData = file.getBytes();

        byte[] encryptedData =
                FileEncryptionUtil.encrypt(fileData);

        Files.write(destination, encryptedData);

        Document document = new Document();

        document.setFileName(originalName);
        document.setContentType(file.getContentType());
        document.setFileSize(file.getSize());
        document.setStoragePath(destination.toString());

        try {
            return documentRepository.save(document);
        } catch (RuntimeException exception) {
            Files.deleteIfExists(destination);
            throw exception;
        }
    }

    // Get document by ID
    public Document getDocument(Long id) {

        return documentRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Document not found."));
    }

public void deleteDocument(Long id) throws IOException {

    Document document = getDocument(id);

    Path filePath = Path.of(document.getStoragePath());

    Files.deleteIfExists(filePath);

    documentRepository.delete(document);
}

}
