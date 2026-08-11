package com.qpss.repository;

import com.qpss.model.ExamCoRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamCoRuleRepository extends JpaRepository<ExamCoRule, Long> {
    List<ExamCoRule> findByExamType(String examType);
}
