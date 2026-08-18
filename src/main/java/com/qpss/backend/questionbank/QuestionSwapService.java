package com.qpss.backend.questionbank;

import com.qpss.common.domain.PaperSection;
import com.qpss.backend.paper.GeneratedPaper;
import com.qpss.backend.paper.PaperQuestion;
import com.qpss.backend.paper.GeneratedPaperRepository;
import com.qpss.backend.paper.PaperQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
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

        List<Long> usedQuestionIds = new java.util.ArrayList<>(
                paperQuestionRepo.findByPaperIdOrderByQuestionNumberAscChoiceLabelAsc(paperId).stream()
                        .map(PaperQuestion::getQuestionId)
                        .collect(Collectors.toList()));

        List<Long> sessionPaperIds = paperRepo.findBySessionId(paper.getSessionId()).stream()
                .map(GeneratedPaper::getId)
                .collect(Collectors.toList());
        if (!sessionPaperIds.isEmpty()) {
            Set<Long> otherPaperQuestionIds = paperQuestionRepo.findByPaperIdIn(sessionPaperIds).stream()
                    .map(PaperQuestion::getQuestionId)
                    .collect(Collectors.toSet());
            usedQuestionIds.addAll(otherPaperQuestionIds);
        }
        Set<Long> excludedIds = new HashSet<>(usedQuestionIds);
        excludedIds.remove(oldQuestionId);

        List<Question> alternatives = questionRepo.findBySessionIdOrderByUnitAscSerialNoAsc(paper.getSessionId())
                .stream()
                .filter(q -> q.getUnit() == oldQuestion.getUnit()
                        && q.getMarks() == oldQuestion.getMarks()
                        && q.getT() == oldQuestion.getT()
                        && (!partB || Objects.equals(q.getRbt(), oldQuestion.getRbt()))
                        && !excludedIds.contains(q.getId()))
                .collect(Collectors.toList());

        if (alternatives.isEmpty()) {
            throw new IllegalStateException("No valid alternative question found to preserve distribution.");
        }

        Collections.shuffle(alternatives);
        Question newQuestion = alternatives.get(0);

        oldPq.setQuestionId(newQuestion.getId());
        if (!partB) {
            oldPq.setDisplayRbt(newQuestion.getRbt());
        }
        paperQuestionRepo.save(oldPq);

        return newQuestion;
    }
}