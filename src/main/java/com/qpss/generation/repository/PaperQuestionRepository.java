package com.qpss.generation.repository;

import com.qpss.generation.model.PaperQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaperQuestionRepository extends JpaRepository<PaperQuestion, Long> {

    List<PaperQuestion> findByPaperIdOrderByQuestionNumberAscChoiceLabelAsc(Long paperId);

    List<PaperQuestion> findByPaperIdIn(List<Long> paperIds);

    void deleteByPaperIdIn(List<Long> paperIds);
}