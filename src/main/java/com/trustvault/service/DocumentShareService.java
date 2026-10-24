
package com.trustvault.service;

import com.trustvault.entity.DocumentShare;
import com.trustvault.model.Document;
import com.trustvault.repository.DocumentShareRepository;
import com.trustvault.repository.DocumentRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentShareService {

    private final DocumentShareRepository documentShareRepository;
    private final DocumentRepository documentRepository;

    public DocumentShareService(
            DocumentShareRepository documentShareRepository,
            DocumentRepository documentRepository) {

        this.documentShareRepository = documentShareRepository;
        this.documentRepository = documentRepository;
    }

    // Share a document
    public DocumentShare shareDocument(
            Long documentId,
            String sharedBy,
            String sharedWith,
            String permission) {

        if (sharedWith == null || sharedWith.isBlank()) {
            throw new IllegalArgumentException(
                    "Recipient email is required.");
        }

        if (sharedBy == null || sharedBy.isBlank()) {
            throw new IllegalArgumentException(
                    "Owner email is required.");
        }

        if (permission == null) {
            throw new IllegalArgumentException(
                    "Sharing permission is required.");
        }

        sharedBy = sharedBy.trim();
        sharedWith = sharedWith.trim();
        permission = permission.trim().toUpperCase();

        if (sharedBy.equalsIgnoreCase(sharedWith)) {
            throw new IllegalArgumentException(
                    "You cannot share a document with yourself.");
        }

        if (!"VIEW".equals(permission)
                && !"DOWNLOAD".equals(permission)) {

            throw new IllegalArgumentException(
                    "Invalid sharing permission.");
        }

        // Find document
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Document not found."));

        // Verify document owner
        if (document.getOwnerEmail() == null
                || !document.getOwnerEmail()
                        .equalsIgnoreCase(sharedBy)) {

            throw new IllegalArgumentException(
                    "You can share only your own documents.");
        }

        // Create sharing permission
        DocumentShare documentShare = new DocumentShare(
                documentId,
                sharedBy,
                sharedWith,
                permission
        );

        return documentShareRepository.save(documentShare);
    }

    // Get documents shared by a user
    public List<DocumentShare> getSharedByUser(String username) {

        return documentShareRepository.findBySharedBy(username);
    }

    // Get documents shared with a user
    public List<DocumentShare> getSharedWithUser(String username) {

        return documentShareRepository.findBySharedWith(username);
    }

    // Get all shared documents
    public List<DocumentShare> getAllShares() {

        return documentShareRepository.findAll();
    }

    // Revoke sharing permission
    public void revokeShare(Long shareId, String username) {

        DocumentShare share = documentShareRepository
                .findByIdAndSharedBy(shareId, username)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "You cannot revoke this sharing permission."));

        documentShareRepository.delete(share);
    }

    // Get shared document after checking recipient permission
    // Allows VIEW and DOWNLOAD permissions
    public Document getSharedDocument(
            Long shareId,
            String username) {

        DocumentShare share = documentShareRepository
                .findByIdAndSharedWith(shareId, username)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Sharing permission not found or access denied."));

        String permission = share.getPermission();

        if (!"VIEW".equalsIgnoreCase(permission)
                && !"DOWNLOAD".equalsIgnoreCase(permission)) {

            throw new IllegalArgumentException(
                    "You do not have permission to view this document.");
        }

        // Find document
        Document document = documentRepository
                .findById(share.getDocumentId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Shared document not found."));

        // Verify document owner
        if (document.getOwnerEmail() == null
                || !document.getOwnerEmail()
                        .equalsIgnoreCase(share.getSharedBy())) {

            throw new IllegalArgumentException(
                    "Document owner verification failed.");
        }

        return document;
    }

    // Get shared document for DOWNLOAD permission only
    public Document getSharedDocumentForDownload(
            Long shareId,
            String username) {

        DocumentShare share = documentShareRepository
                .findByIdAndSharedWith(shareId, username)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Sharing permission not found or access denied."));

        // Only DOWNLOAD permission is allowed
        if (!"DOWNLOAD".equalsIgnoreCase(share.getPermission())) {

            throw new IllegalArgumentException(
                    "You do not have permission to download this document.");
        }

        // Find document
        Document document = documentRepository
                .findById(share.getDocumentId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Shared document not found."));

        // Verify document owner
        if (document.getOwnerEmail() == null
                || !document.getOwnerEmail()
                        .equalsIgnoreCase(share.getSharedBy())) {

            throw new IllegalArgumentException(
                    "Document owner verification failed.");
        }

        return document;
    }
}
