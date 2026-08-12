package com.qpss.repository;

import com.qpss.model.ExamSectionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ExamSectionConfigRepository extends JpaRepository<ExamSectionConfig, Long> {
    Optional<ExamSectionConfig> findByExamTypeAndMarks(String examType, Integer marks);
}
