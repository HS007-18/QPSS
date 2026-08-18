package com.qpss.questionbank.repository;

import com.qpss.questionbank.model.SourceDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SourceDocumentRepository extends JpaRepository<SourceDocument, Long> {

    boolean existsByChecksumAndImportBatch_SessionId(String checksum, Long sessionId);

    List<SourceDocument> findByImportBatch_SessionIdIn(List<Long> sessionIds);

    void deleteByImportBatch_SessionIdIn(List<Long> sessionIds);
}
