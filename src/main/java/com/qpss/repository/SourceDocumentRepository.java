package com.qpss.repository;

import com.qpss.model.SourceDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceDocumentRepository extends JpaRepository<SourceDocument, Long> {
    boolean existsByChecksum(String checksum);

    boolean existsByChecksumAndImportBatch_SessionId(String checksum, Long sessionId);
}
