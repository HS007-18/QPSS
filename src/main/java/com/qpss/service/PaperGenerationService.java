package com.qpss.service;

import com.qpss.entity.GeneratedPaper;
import com.qpss.domain.generation.DiversityAnalyzer;
import com.qpss.domain.ExamType;
import com.qpss.entity.Question;
import com.qpss.domain.distribution.DistributionPlan;
import com.qpss.domain.selection.PairingEngine;
import com.qpss.domain.selection.SelectionEngine;
import com.qpss.domain.validation.ValidationEngine;
import com.qpss.document.model.HeaderMetadata;
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
import java.util.stream.Collectors;
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
    private final DocumentMetadataService documentMetadataService;

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
    public GenerationResult generate(String examTypeStr, Long subjectId, Long sessionId, int numSets, String formatStr, String duration) {
        ExamType examType = ExamType.from(examTypeStr);
        com.qpss.domain.ExamFormat format = com.qpss.domain.ExamFormat.from(formatStr);
        DistributionPlan plan = examConfigService.getDistributionPlan(examType, format);
        String diversityWarning = diversityAnalyzer.check(plan, subjectId, numSets);
        PairingEngine.PairingMode pairingMode = examType.isCrossHalf()
                ? PairingEngine.PairingMode.CROSS_HALF : PairingEngine.PairingMode.SAME_HALF;

        List<GeneratedSet> sets = new ArrayList<>();
        Set<Long> usedIds = new HashSet<>();
        List<SelectionEngine.SelectionShortage> allShortages = new ArrayList<>();

        for (int i = 0; i < numSets; i++) {
            SelectionEngine.SelectionResult selection = selectionEngine.select(
                    plan, subjectId, sessionId, usedIds, pairingMode);

            if (!selection.isSuccessful()) {
                allShortages.addAll(selection.getShortages());
                if (i == 0) {
                    return new GenerationResult(Collections.emptyList(), allShortages, null, false);
                }
                SelectionEngine.SelectionResult fallback = selectionEngine.select(
                        plan, subjectId, sessionId, Collections.emptySet(), pairingMode);
                if (!fallback.isSuccessful()) {
                    allShortages.addAll(fallback.getShortages());
                    return new GenerationResult(Collections.emptyList(), allShortages, null, false);
                }
                selection = fallback;
            }

            int partBMarks = plan.getSections().stream()
                    .mapToInt(s -> s.getMarks())
                    .filter(m -> m == 16 || m == 20)
                    .max().orElse(16);

            List<PairingEngine.QuestionPair> pairs = pairingEngine.createPairs(
                    selection.getPartBQuestions(partBMarks), pairingMode);

            ValidationEngine.ValidationResult validation = validationEngine.validate(
                    examType.name(), subjectId, sessionId, format.name(),
                    selection.getTwoMarkQuestions(), pairs);

            if (!validation.isValid()) {
                log.error("Validation failed for set {}: {}", i + 1, validation.getFailures());
                throw new IllegalStateException(
                        "Validation failed for set " + (i + 1) + ": " + validation.getFailures());
            }

            Map<Long, String> sectionARbt = selection.getTwoMarkQuestions().stream()
                    .collect(Collectors.toMap(Question::getId, Question::getRbt, (a, b) -> a));

            String label = String.valueOf((char) ('A' + i));
            GeneratedPaper paper = draftService.savePaper(sessionId, subjectId, examType.name(), label);
            paper.setDuration(duration);
            draftService.savePaperQuestions(paper.getId(), selection.getTwoMarkQuestions(), pairs, sectionARbt);

            selection.getTwoMarkQuestions().forEach(q -> usedIds.add(q.getId()));
            selection.getPartBQuestions(partBMarks).forEach(q -> usedIds.add(q.getId()));

            // Extract exam metadata from first question's source document for first set
            if (i == 0 && !selection.getTwoMarkQuestions().isEmpty()) {
                Question firstQ = selection.getTwoMarkQuestions().get(0);
                if (firstQ.getSourceDocumentId() != null) {
                    HeaderMetadata metadata = documentMetadataService.extractMetadata(firstQ.getSourceDocumentId());
                    
                    paper.setExamSession(metadata.getExamSession());
                    paper.setExamTitle(metadata.getExamTitle());
                }
            }

            sets.add(new GeneratedSet(paper, selection.getTwoMarkQuestions(), pairs, sectionARbt));
        }

        draftService.deleteDraftsExcluding(sessionId, sets.stream().map(s -> s.getPaper().getId()).collect(Collectors.toSet()));

        return new GenerationResult(sets, Collections.emptyList(), diversityWarning, true);
    }

    @Transactional
    public GenerationResult generatePartAOnly(String examTypeStr, Long subjectId, Long sessionId, 
                                              int numSets, String duration, Map<String, Integer> topicCounts,
                                              PartAOnlyGenerationService partAOnlyGenService) {
        ExamType examType = ExamType.from(examTypeStr);
        List<GeneratedSet> sets = new ArrayList<>();
        List<SelectionEngine.SelectionShortage> allShortages = new ArrayList<>();

        for (int i = 0; i < numSets; i++) {
            List<Question> selectedQuestions;
            try {
                selectedQuestions = partAOnlyGenService.generatePartASet(subjectId, topicCounts);
            } catch (Exception e) {
                log.error("Failed to generate FORMAT_3 Part A set", e);
                SelectionEngine.SelectionShortage shortage = new SelectionEngine.SelectionShortage(0, 2, 0, 50, 0, 50);
                allShortages.add(shortage);
                return new GenerationResult(Collections.emptyList(), allShortages, null, false);
            }

            Map<Long, String> sectionARbt = selectedQuestions.stream()
                    .collect(Collectors.toMap(Question::getId, Question::getRbt, (a, b) -> a));

            String label = String.valueOf((char) ('A' + i));
            GeneratedPaper paper = draftService.savePaper(sessionId, subjectId, examType.name(), label);
            paper.setDuration(duration);
            draftService.savePaperQuestionsPartAOnly(paper.getId(), selectedQuestions);

            if (i == 0 && !selectedQuestions.isEmpty()) {
                Question firstQ = selectedQuestions.get(0);
                if (firstQ.getSourceDocumentId() != null) {
                    HeaderMetadata metadata = documentMetadataService.extractMetadata(firstQ.getSourceDocumentId());
                    paper.setExamSession(metadata.getExamSession());
                    paper.setExamTitle(metadata.getExamTitle());
                }
            }

            sets.add(new GeneratedSet(paper, selectedQuestions, Collections.emptyList(), sectionARbt));
        }

        draftService.deleteDraftsExcluding(sessionId, sets.stream().map(s -> s.getPaper().getId()).collect(Collectors.toSet()));

        return new GenerationResult(sets, Collections.emptyList(), null, true);
    }

    @Transactional
    public void finalizePaper(Long paperId, Long sessionId) {
        GeneratedPaper paper = getPaperById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));
        if (!paper.getSessionId().equals(sessionId)) {
            throw new IllegalArgumentException("Paper does not belong to session");
        }
        paper.setIsFinal(true);
        draftService.save(paper);
    }

    public java.util.Optional<GeneratedPaper> getPaperById(Long paperId) {
        return draftService.findById(paperId);
    }

    public java.util.Optional<GeneratedPaper> getPaperByIdAndSessionId(Long paperId, Long sessionId) {
        return draftService.findById(paperId)
                .filter(p -> p.getSessionId().equals(sessionId));
    }

    public Question swapQuestion(Long paperId, Long oldQuestionId, Long sessionId) {
        GeneratedPaper paper = getPaperById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));
        if (!paper.getSessionId().equals(sessionId)) {
            throw new IllegalArgumentException("Paper does not belong to session");
        }
        return swapService.swapQuestion(paperId, oldQuestionId);
    }
}