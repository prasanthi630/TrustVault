
package com.trustvault.service;

import com.trustvault.entity.DocumentShare;
import com.trustvault.repository.DocumentShareRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentShareService {

    private final DocumentShareRepository documentShareRepository;

    public DocumentShareService(
            DocumentShareRepository documentShareRepository) {

        this.documentShareRepository = documentShareRepository;
    }

    // Share a document
    public DocumentShare shareDocument(
            Long documentId,
            String sharedBy,
            String sharedWith,
            String permission) {

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

    // Delete sharing permission
    public void revokeShare(Long shareId) {

        documentShareRepository.deleteById(shareId);
    }
}
