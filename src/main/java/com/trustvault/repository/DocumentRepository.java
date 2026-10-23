
package com.trustvault.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.trustvault.model.Document;

public interface DocumentRepository extends JpaRepository<Document, Long> {
}
