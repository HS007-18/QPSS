package com.qpss.examconfig.repository;

import com.qpss.examconfig.model.ExamConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamConfigRepository extends JpaRepository<ExamConfig, Long> {
    List<ExamConfig> findByExamTypeOrderByMarksAscUnitAsc(String examType);
}
