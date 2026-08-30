package com.qpss.repository;

import com.qpss.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySubjectIdAndUnitAndMarksAndT(
            Long subjectId, Integer unit, Integer marks, Integer t);

    List<Question> findBySubjectIdAndUnitAndTopicAndMarks(
            Long subjectId, Integer unit, Integer topic, Integer marks);
            
    List<Question> findBySubjectIdAndUnitAndMarks(
            Long subjectId, Integer unit, Integer marks);

    @org.springframework.data.jpa.repository.Query("SELECT q.unit, q.topic, COUNT(q) FROM Question q WHERE q.subjectId = :subjectId AND q.sessionId = :sessionId GROUP BY q.unit, q.topic ORDER BY q.unit, q.topic")
    List<Object[]> countQuestionsByUnitAndTopic(
            @org.springframework.data.repository.query.Param("subjectId") Long subjectId,
            @org.springframework.data.repository.query.Param("sessionId") Long sessionId);

    int countBySubjectIdAndUnitAndMarks(Long subjectId, Integer unit, Integer marks);

    long countBySubjectId(Long subjectId);

    List<Question> findBySubjectIdOrderByUnitAscSerialNoAsc(Long subjectId);

    @Modifying
    @Transactional
    void deleteBySubjectId(Long subjectId);
}