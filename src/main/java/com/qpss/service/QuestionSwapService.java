package com.qpss.service;

import com.qpss.domain.PaperSection;
import com.qpss.model.GeneratedPaper;
import com.qpss.model.PaperQuestion;
import com.qpss.model.Question;
import com.qpss.repository.GeneratedPaperRepository;
import com.qpss.repository.PaperQuestionRepository;
import com.qpss.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionSwapService {

    private final GeneratedPaperRepository paperRepo;
    private final PaperQuestionRepository paperQuestionRepo;
    private final QuestionRepository questionRepo;

    @Transactional
    public Question swapQuestion(Long paperId, Long oldQuestionId) {
        GeneratedPaper paper = paperRepo.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));

        if (Boolean.TRUE.equals(paper.getIsFinal())) {
            throw new IllegalStateException("Cannot edit finalized paper");
        }

        PaperQuestion oldPq = paperQuestionRepo.findByPaperIdOrderByQuestionNumberAscChoiceLabelAsc(paperId).stream()
                .filter(pq -> pq.getQuestionId().equals(oldQuestionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Question not in paper"));

        Question oldQuestion = questionRepo.findById(oldQuestionId)
                .orElseThrow(() -> new IllegalArgumentException("Question not found"));

        boolean partB = PaperSection.SECTION_B == PaperSection.from(oldPq.getSection());

        List<Long> currentQuestionIds = paperQuestionRepo.findByPaperIdOrderByQuestionNumberAscChoiceLabelAsc(paperId)
                .stream().map(PaperQuestion::getQuestionId).collect(Collectors.toList());

        List<Question> alternatives = questionRepo.findBySessionIdOrderByUnitAscSerialNoAsc(paper.getSessionId())
                .stream()
                .filter(q -> q.getUnit() == oldQuestion.getUnit()
                        && q.getMarks() == oldQuestion.getMarks()
                        && q.getT() == oldQuestion.getT()
                        && (!partB || Objects.equals(q.getRbt(), oldQuestion.getRbt()))
                        && !currentQuestionIds.contains(q.getId()))
                .collect(Collectors.toList());

        if (alternatives.isEmpty()) {
            throw new IllegalStateException("No valid alternative question found to preserve distribution.");
        }

        Collections.shuffle(alternatives);
        Question newQuestion = alternatives.get(0);

        oldPq.setQuestionId(newQuestion.getId());
        paperQuestionRepo.save(oldPq);

        return newQuestion;
    }
}
