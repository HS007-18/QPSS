package com.qpss.backend.paper;
import com.qpss.common.domain.ExamType;
import com.qpss.backend.questionbank.Question;
import com.qpss.backend.examconfig.ExamConfigService;
import com.qpss.common.domain.DistributionPlan;
import com.qpss.backend.selection.PairingEngine;
import com.qpss.backend.selection.SelectionEngine;
import com.qpss.backend.selection.ValidationEngine;
import com.qpss.backend.questionbank.QuestionSwapService;
import com.qpss.documentextraction.model.HeaderMetadata;
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
    public GenerationResult generate(String examType, Long subjectId, Long sessionId, int numSets, String format, String duration) {
        ExamType.from(examType);
        DistributionPlan plan = examConfigService.getDistributionPlan(examType, format);
        String diversityWarning = diversityAnalyzer.check(plan, subjectId, numSets);
        PairingEngine.PairingMode pairingMode = ExamType.from(examType).isCrossHalf()
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
                    examType, subjectId, sessionId, format,
                    selection.getTwoMarkQuestions(), pairs);

            if (!validation.isValid()) {
                log.error("Validation failed for set {}: {}", i + 1, validation.getFailures());
                throw new IllegalStateException(
                        "Validation failed for set " + (i + 1) + ": " + validation.getFailures());
            }

            Map<Long, String> sectionARbt = selection.getTwoMarkQuestions().stream()
                    .collect(Collectors.toMap(Question::getId, Question::getRbt, (a, b) -> a));

            String label = String.valueOf((char) ('A' + i));
            GeneratedPaper paper = draftService.savePaper(sessionId, subjectId, examType, label);
            paper.setDuration(duration);
            draftService.savePaperQuestions(paper.getId(), selection.getTwoMarkQuestions(), pairs, sectionARbt);

            selection.getTwoMarkQuestions().forEach(q -> usedIds.add(q.getId()));
            selection.getPartBQuestions(partBMarks).forEach(q -> usedIds.add(q.getId()));

            // Extract exam metadata from first question's source document for first set
            if (i == 0 && !selection.getTwoMarkQuestions().isEmpty()) {
                Question firstQ = selection.getTwoMarkQuestions().get(0);
                if (firstQ.getSourceDocumentId() != null) {
                    HeaderMetadata metadata = extractMetadata(firstQ.getSourceDocumentId());
                    paper.setExamSession(metadata.getExamSession());
                    paper.setExamTitle(metadata.getExamTitle());
                }
            }

            sets.add(new GeneratedSet(paper, selection.getTwoMarkQuestions(), pairs, sectionARbt));
        }

        draftService.deleteDraftsExcluding(sessionId, sets.stream().map(s -> s.getPaper().getId()).collect(Collectors.toSet()));

        return new GenerationResult(sets, Collections.emptyList(), diversityWarning, true);
    }

    private HeaderMetadata extractMetadata(Long sourceDocumentId) {
        try {
            // This would require a way to load the document and extract metadata
            // For now, return empty metadata - the renderer will handle fallback
            return HeaderMetadata.builder().courseOutcomes(new ArrayList<>()).build();
        } catch (Exception e) {
            log.warn("Failed to extract metadata from source document {}", sourceDocumentId, e);
            return HeaderMetadata.builder().courseOutcomes(new ArrayList<>()).build();
        }
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