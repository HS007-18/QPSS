package com.qpss.backend.questionbank;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySubjectIdAndSessionIdAndUnitAndMarksAndT(
            Long subjectId, Long sessionId, Integer unit, Integer marks, Integer t);

    int countBySessionIdAndUnitAndMarks(Long sessionId, Integer unit, Integer marks);

    List<Question> findBySessionIdOrderByUnitAscSerialNoAsc(Long sessionId);

    void deleteBySubjectId(Long subjectId);
}