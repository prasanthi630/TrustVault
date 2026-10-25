
package com.trustvault.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.trustvault.entity.DocumentShare;

public interface DocumentShareRepository
        extends JpaRepository<DocumentShare, Long> {

    // Existing methods
    List<DocumentShare> findBySharedBy(String sharedBy);

    List<DocumentShare> findBySharedWith(String sharedWith);

    Optional<DocumentShare> findByIdAndSharedBy(
            Long id,
            String sharedBy);

    Optional<DocumentShare> findByIdAndSharedWith(
            Long id,
            String sharedWith);

    // Case-insensitive email lookups
    List<DocumentShare> findBySharedByIgnoreCase(String sharedBy);

    List<DocumentShare> findBySharedWithIgnoreCase(String sharedWith);

    Optional<DocumentShare> findByIdAndSharedByIgnoreCase(
            Long id,
            String sharedBy);

    Optional<DocumentShare> findByIdAndSharedWithIgnoreCase(
            Long id,
            String sharedWith);

    // Find sharing records linked to document IDs
    List<DocumentShare> findByDocumentIdIn(
            List<Long> documentIds);
}
