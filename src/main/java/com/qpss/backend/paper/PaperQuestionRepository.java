package com.qpss.backend.paper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
public interface PaperQuestionRepository extends JpaRepository<PaperQuestion, Long> {

    List<PaperQuestion> findByPaperIdOrderByQuestionNumberAscChoiceLabelAsc(Long paperId);

    List<PaperQuestion> findByPaperIdIn(List<Long> paperIds);

    @Modifying
    @Transactional
    void deleteByPaperIdIn(List<Long> paperIds);
}