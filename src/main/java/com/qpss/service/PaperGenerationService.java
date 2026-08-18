package com.qpss.service;

import com.qpss.domain.ExamType;
import com.qpss.model.GeneratedPaper;
import com.qpss.model.Question;
import com.qpss.service.selection.PairingEngine;
import com.qpss.service.selection.SelectionEngine;
import com.qpss.service.selection.ValidationEngine;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PaperGenerationService {

    private static final Logger log = LoggerFactory.getLogger(PaperGenerationService.class);

    private final SelectionEngine selectionEngine;
    private final PairingEngine pairingEngine;
    private final ValidationEngine validationEngine;
    private final PaperDraftPersistenceService draftService;
    private final QuestionSwapService swapService;
    private final DiversityAnalyzer diversityAnalyzer;
    private final ExamConfigService examConfigService;

    @Data
    @AllArgsConstructor
    public static class GeneratedSet {
        private GeneratedPaper paper;
        private List<Question> sectionA;
        private List<PairingEngine.QuestionPair> sectionB;
        private Map<Long, String> sectionARbt;
    }

    @Data
    @AllArgsConstructor
    public static class GenerationResult {
        private List<GeneratedSet> sets;
        private List<SelectionEngine.SelectionShortage> shortages;
        private String diversityWarning;
        private boolean successful;
    }

    @Transactional
    public GenerationResult generate(String examType, Long subjectId, Long sessionId, int numSets, String format) {
        draftService.deleteDrafts(sessionId);

        com.qpss.service.distribution.DistributionPlan plan = examConfigService.getDistributionPlan(examType, format);
        String diversityWarning = diversityAnalyzer.check(plan, sessionId, numSets);
        PairingEngine.PairingMode pairingMode = ExamType.from(examType).isCrossHalf()
                ? PairingEngine.PairingMode.CROSS_HALF : PairingEngine.PairingMode.SAME_HALF;

        List<GeneratedSet> sets = new ArrayList<>();
        Set<Long> usedIds = new HashSet<>();

        for (int i = 0; i < numSets; i++) {
            SelectionEngine.SelectionResult selection = selectionEngine.select(
                    plan, subjectId, sessionId, usedIds, pairingMode);

            if (!selection.isSuccessful()) {
                if (i == 0) {
                    return new GenerationResult(
                            Collections.emptyList(), selection.getShortages(), null, false);
                }
                SelectionEngine.SelectionResult fallback = selectionEngine.select(
                        plan, subjectId, sessionId, Collections.emptySet(), pairingMode);
                if (!fallback.isSuccessful()) {
                    return new GenerationResult(
                            Collections.emptyList(), fallback.getShortages(), null, false);
                }
                selection = fallback;
            }

            int partBMarks = plan.getSections().stream()
                    .mapToInt(s -> s.getMarks())
                    .filter(m -> m == 16 || m == 20)
                    .findFirst().orElse(16);

            List<PairingEngine.QuestionPair> pairs = pairingEngine.createPairs(
                    selection.getPartBQuestions(partBMarks), pairingMode);

            ValidationEngine.ValidationResult validation = validationEngine.validate(
                    examType, subjectId, sessionId, format,
                    selection.getTwoMarkQuestions(), pairs);

            if (!validation.isValid()) {
                log.error("Validation failed for set {}: {}", i + 1, validation.getFailures());
                throw new IllegalStateException(
                        "Validation failed for set " + (i + 1) + ": " + validation.getFailures());
            }

            Map<Long, String> sectionARbt = selection.getTwoMarkQuestions().stream()
                    .collect(java.util.stream.Collectors.toMap(Question::getId, Question::getRbt));

            String label = String.valueOf((char) ('A' + i));
            GeneratedPaper paper = draftService.savePaper(sessionId, subjectId, examType, label);
            draftService.savePaperQuestions(paper.getId(), selection.getTwoMarkQuestions(), pairs, sectionARbt);

            selection.getTwoMarkQuestions().forEach(q -> usedIds.add(q.getId()));
            selection.getPartBQuestions(partBMarks).forEach(q -> usedIds.add(q.getId()));

            sets.add(new GeneratedSet(paper, selection.getTwoMarkQuestions(), pairs, sectionARbt));
        }

        return new GenerationResult(sets, Collections.emptyList(), diversityWarning, true);
    }

    @Transactional
    public void finalizePaper(Long paperId) {
        GeneratedPaper paper = getPaperById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));
        paper.setIsFinal(true);
        draftService.save(paper);
    }

    public java.util.Optional<GeneratedPaper> getPaperById(Long paperId) {
        return draftService.findById(paperId);
    }

    public Question swapQuestion(Long paperId, Long oldQuestionId) {
        return swapService.swapQuestion(paperId, oldQuestionId);
    }
}
