
package com.trustvault.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trustvault.model.Document;

public interface DocumentRepository
        extends JpaRepository<Document, Long> {

    // Get documents uploaded by a specific user
    List<Document> findByOwnerEmail(String ownerEmail);

}
