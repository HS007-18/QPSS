package com.qpss.repository;

import com.qpss.entity.GeneratedPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
public interface GeneratedPaperRepository extends JpaRepository<GeneratedPaper, Long> {

    List<GeneratedPaper> findByBatchId(String batchId);

    List<GeneratedPaper> findBySubjectId(Long subjectId);

    @Modifying
    @Transactional
    void deleteByBatchId(String batchId);

    @Modifying
    @Transactional
    void deleteBySubjectId(Long subjectId);
}