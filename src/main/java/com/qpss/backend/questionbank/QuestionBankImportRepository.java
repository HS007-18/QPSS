package com.qpss.backend.questionbank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
public interface QuestionBankImportRepository extends JpaRepository<QuestionBankImport, Long> {

    long countBySubjectId(Long subjectId);

    @Modifying
    @Transactional
    void deleteBySessionIdIn(List<Long> sessionIds);
}
