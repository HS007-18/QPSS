package com.qpss.repository;

import com.qpss.model.SourceDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceDocumentRepository extends JpaRepository<SourceDocument, Long> {
    boolean existsByImportBatchIdAndChecksum(Long importBatchId, String checksum);
}
