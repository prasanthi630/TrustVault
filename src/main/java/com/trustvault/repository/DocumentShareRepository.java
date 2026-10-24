
package com.trustvault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.trustvault.entity.DocumentShare;

import java.util.List;
import java.util.Optional;

public interface DocumentShareRepository
        extends JpaRepository<DocumentShare, Long> {

    List<DocumentShare> findBySharedBy(String sharedBy);

    List<DocumentShare> findBySharedWith(String sharedWith);

    Optional<DocumentShare> findByIdAndSharedBy(
            Long id,
            String sharedBy
    );

    Optional<DocumentShare> findByIdAndSharedWith(
            Long id,
            String sharedWith
    );
}
