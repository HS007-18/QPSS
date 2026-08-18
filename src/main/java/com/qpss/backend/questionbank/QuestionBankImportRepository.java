package com.qpss.backend.questionbank;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionBankImportRepository extends JpaRepository<QuestionBankImport, Long> {

    void deleteBySessionIdIn(List<Long> sessionIds);
}
