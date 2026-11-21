package com.qpss.repository;

import com.qpss.entity.SourceDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
public interface SourceDocumentRepository extends JpaRepository<SourceDocument, Long> {

    boolean existsByChecksumAndImportBatch_SubjectId(String checksum, Long subjectId);

    List<SourceDocument> findByImportBatch_SubjectId(Long subjectId);

    @Modifying
    @Transactional
    void deleteByImportBatch_SubjectId(Long subjectId);
}
