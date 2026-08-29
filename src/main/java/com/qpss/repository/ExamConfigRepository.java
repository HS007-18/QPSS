package com.qpss.repository;

import com.qpss.entity.ExamConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ExamConfigRepository extends JpaRepository<ExamConfig, Long> {
    List<ExamConfig> findByExamTypeOrderByMarksAscUnitAsc(String examType);
}
