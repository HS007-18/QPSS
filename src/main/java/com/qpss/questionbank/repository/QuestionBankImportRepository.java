package com.qpss.questionbank.repository;

import com.qpss.questionbank.model.QuestionBankImport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionBankImportRepository extends JpaRepository<QuestionBankImport, Long> {

    void deleteBySessionIdIn(List<Long> sessionIds);
}
