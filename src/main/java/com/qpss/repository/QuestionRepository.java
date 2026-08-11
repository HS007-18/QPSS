package com.qpss.repository;

import com.qpss.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySubjectIdAndSessionIdAndUnitAndMarks(
            Long subjectId, Long sessionId, Integer unit, Integer marks);

    int countBySessionIdAndUnitAndMarks(Long sessionId, Integer unit, Integer marks);
    void deleteBySubjectId(Long subjectId);
}
