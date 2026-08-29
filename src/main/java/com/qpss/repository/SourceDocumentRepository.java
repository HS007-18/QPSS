package com.qpss.repository;

import com.qpss.entity.SourceDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
public interface SourceDocumentRepository extends JpaRepository<SourceDocument, Long> {

    boolean existsByChecksumAndImportBatch_SessionId(String checksum, Long sessionId);

    List<SourceDocument> findByImportBatch_SessionIdIn(List<Long> sessionIds);

    @Modifying
    @Transactional
    void deleteByImportBatch_SessionIdIn(List<Long> sessionIds);
}
