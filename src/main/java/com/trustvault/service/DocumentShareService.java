
package com.trustvault.service;

import com.trustvault.entity.DocumentShare;
import com.trustvault.model.Document;
import com.trustvault.repository.DocumentShareRepository;
import com.trustvault.repository.DocumentRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

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
    @Transactional
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

        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException(
                    "Sharing permission is required.");
        }

        sharedBy = sharedBy.trim().toLowerCase(Locale.ROOT);
        sharedWith = sharedWith.trim().toLowerCase(Locale.ROOT);
        permission = permission.trim().toUpperCase(Locale.ROOT);

        if (sharedBy.equals(sharedWith)) {
            throw new IllegalArgumentException(
                    "You cannot share a document with yourself.");
        }

        if (!"VIEW".equals(permission)
                && !"DOWNLOAD".equals(permission)) {

            throw new IllegalArgumentException(
                    "Invalid sharing permission.");
        }

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Document not found."));

        // Only the document owner can share it.
        if (document.getOwnerEmail() == null
                || !document.getOwnerEmail().equalsIgnoreCase(sharedBy)) {

            throw new IllegalArgumentException(
                    "You can share only your own documents.");
        }

        DocumentShare documentShare = new DocumentShare(
                documentId,
                sharedBy,
                sharedWith,
                permission);

        return documentShareRepository.save(documentShare);
    }

    // Documents shared by this user
    public List<DocumentShare> getSharedByUser(String username) {

        return documentShareRepository
                .findBySharedByIgnoreCase(username.trim());
    }

    // Documents shared with this user
    public List<DocumentShare> getSharedWithUser(String username) {

        return documentShareRepository
                .findBySharedWithIgnoreCase(username.trim());
    }

    // All sharing records
    public List<DocumentShare> getAllShares() {

        return documentShareRepository.findAll();
    }

    // Revoke only a share created by the current owner
    @Transactional
    public void revokeShare(Long shareId, String username) {

        DocumentShare share = documentShareRepository
                .findByIdAndSharedByIgnoreCase(
                        shareId,
                        username.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "You cannot revoke this sharing permission."));

        documentShareRepository.delete(share);
    }

    // Get a shared document for VIEW or DOWNLOAD permission
    public Document getSharedDocument(
            Long shareId,
            String username) {

        DocumentShare share = documentShareRepository
                .findByIdAndSharedWithIgnoreCase(
                        shareId,
                        username.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Sharing permission not found or access denied."));

        String permission = share.getPermission();

        if (!"VIEW".equalsIgnoreCase(permission)
                && !"DOWNLOAD".equalsIgnoreCase(permission)) {

            throw new IllegalArgumentException(
                    "You do not have permission to view this document.");
        }

        return findAndVerifyDocument(share);
    }

    // Get a shared document for DOWNLOAD permission only
    public Document getSharedDocumentForDownload(
            Long shareId,
            String username) {

        DocumentShare share = documentShareRepository
                .findByIdAndSharedWithIgnoreCase(
                        shareId,
                        username.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Sharing permission not found or access denied."));

        if (!"DOWNLOAD".equalsIgnoreCase(share.getPermission())) {

            throw new IllegalArgumentException(
                    "You do not have permission to download this document.");
        }

        return findAndVerifyDocument(share);
    }

    // Verify the document exists and belongs to the sharing owner
    private Document findAndVerifyDocument(DocumentShare share) {

        Document document = documentRepository
                .findById(share.getDocumentId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Shared document not found."));

        if (document.getOwnerEmail() == null
                || !document.getOwnerEmail()
                        .equalsIgnoreCase(share.getSharedBy())) {

            throw new IllegalArgumentException(
                    "Document owner verification failed.");
        }

        return document;
    }
}
