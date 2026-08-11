package com.qpss.repository;

import com.qpss.model.PaperQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaperQuestionRepository extends JpaRepository<PaperQuestion, Long> {
    List<PaperQuestion> findByPaperIdOrderByQuestionNumberAscChoiceLabelAsc(Long paperId);
    void deleteByPaperIdIn(List<Long> paperIds);
}
