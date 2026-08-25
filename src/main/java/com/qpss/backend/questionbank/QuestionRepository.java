package com.qpss.backend.questionbank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findBySubjectIdAndUnitAndMarksAndT(
            Long subjectId, Integer unit, Integer marks, Integer t);

    int countBySubjectIdAndUnitAndMarks(Long subjectId, Integer unit, Integer marks);

    long countBySubjectId(Long subjectId);

    List<Question> findBySubjectIdOrderByUnitAscSerialNoAsc(Long subjectId);

    @Modifying
    @Transactional
    void deleteBySubjectId(Long subjectId);
}