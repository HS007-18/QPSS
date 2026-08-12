package com.qpss.service;

import com.qpss.model.*;
import com.qpss.repository.*;
import lombok.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaperGenerationService {

    private final SelectionEngine selectionEngine;
    private final PairingEngine pairingEngine;
    private final ValidationEngine validationEngine;
    private final GeneratedPaperRepository paperRepo;
    private final PaperQuestionRepository paperQuestionRepo;
    private final ExamConfigService examConfigService;
    private final QuestionRepository questionRepo;

    @Data @AllArgsConstructor
    public static class GeneratedSet {
        private GeneratedPaper paper;
        private List<Question> sectionA;
        private List<PairingEngine.QuestionPair> sectionB;
    }

    @Data @AllArgsConstructor
    public static class GenerationResult {
        private List<GeneratedSet> sets;
        private List<SelectionEngine.SelectionShortage> shortages;
        private String diversityWarning;
        private boolean successful;
    }

    @Transactional
    public GenerationResult generate(String examType, Long subjectId, Long sessionId, int numSets, String format) {
        // Clear previous non-finalized drafts for this session
        List<GeneratedPaper> existingDrafts = paperRepo.findBySessionId(sessionId).stream()
                .filter(p -> !Boolean.TRUE.equals(p.getIsFinal()))
                .collect(Collectors.toList());
        if (!existingDrafts.isEmpty()) {
            List<Long> draftIds = existingDrafts.stream().map(GeneratedPaper::getId).collect(Collectors.toList());
            paperQuestionRepo.deleteByPaperIdIn(draftIds);
            paperRepo.deleteAll(existingDrafts);
        }

        com.qpss.service.distribution.DistributionPlan plan = examConfigService.getDistributionPlan(examType, format);
        String diversityWarning = checkDiversity(plan, sessionId, numSets);

        List<GeneratedSet> sets = new ArrayList<>();
        Set<Long> usedIds = new HashSet<>();

        for (int i = 0; i < numSets; i++) {
            SelectionEngine.SelectionResult selection = selectionEngine.select(
                    plan, subjectId, sessionId, usedIds);

            if (!selection.isSuccessful()) {
                if (i == 0) {
                    return new GenerationResult(
                            Collections.emptyList(), selection.getShortages(), null, false);
                }
                SelectionEngine.SelectionResult fallback = selectionEngine.select(
                        plan, subjectId, sessionId, Collections.emptySet());
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
                    selection.getPartBQuestions(partBMarks));

            ValidationEngine.ValidationResult validation = validationEngine.validate(
                    examType, subjectId, sessionId, format,
                    selection.getTwoMarkQuestions(), pairs);

            if (!validation.isValid()) {
                throw new IllegalStateException(
                        "Validation failed for set " + (i + 1) + ": " + validation.getFailures());
            }

            String label = String.valueOf((char) ('A' + i));
            GeneratedPaper paper = paperRepo.save(GeneratedPaper.builder()
                    .sessionId(sessionId)
                    .subjectId(subjectId)
                    .examType(examType)
                    .setLabel(label)
                    .build());

            savePaperQuestions(paper.getId(), selection.getTwoMarkQuestions(), pairs);

            selection.getTwoMarkQuestions().forEach(q -> usedIds.add(q.getId()));
            selection.getPartBQuestions(partBMarks).forEach(q -> usedIds.add(q.getId()));

            sets.add(new GeneratedSet(paper, selection.getTwoMarkQuestions(), pairs));
        }

        return new GenerationResult(sets, Collections.emptyList(), diversityWarning, true);
    }

    private void savePaperQuestions(Long paperId, List<Question> twoMark,
                                    List<PairingEngine.QuestionPair> pairs) {
        for (int i = 0; i < twoMark.size(); i++) {
            paperQuestionRepo.save(PaperQuestion.builder()
                    .paperId(paperId)
                    .questionId(twoMark.get(i).getId())
                    .section("SECTION_A")
                    .questionNumber(i + 1)
                    .build());
        }

        for (PairingEngine.QuestionPair pair : pairs) {
            int qNum = 10 + pair.getPairIndex();
            paperQuestionRepo.save(PaperQuestion.builder()
                    .paperId(paperId)
                    .questionId(pair.getChoiceA().getId())
                    .section("SECTION_B")
                    .questionNumber(qNum)
                    .choiceLabel("a")
                    .pairIndex(pair.getPairIndex())
                    .build());
            paperQuestionRepo.save(PaperQuestion.builder()
                    .paperId(paperId)
                    .questionId(pair.getChoiceB().getId())
                    .section("SECTION_B")
                    .questionNumber(qNum)
                    .choiceLabel("b")
                    .pairIndex(pair.getPairIndex())
                    .build());
        }
    }

    private String checkDiversity(com.qpss.service.distribution.DistributionPlan plan, Long sessionId, int numSets) {
        if (numSets <= 1) return null;

        int minUniqueSets = Integer.MAX_VALUE;

        for (com.qpss.service.distribution.DistributionPlan.SectionPlan section : plan.getSections()) {
            for (com.qpss.service.distribution.DistributionPlan.UnitPlan rule : section.getUnits()) {
                if (rule.getRequiredCount() == 0) continue;
                int poolSize = questionRepo.countBySessionIdAndUnitAndMarks(
                        sessionId, rule.getUnit(), section.getMarks());
                int maxSets = poolSize / rule.getRequiredCount();
                minUniqueSets = Math.min(minUniqueSets, maxSets);
            }
        }

        if (numSets > minUniqueSets) {
            return "Only " + minUniqueSets + " meaningfully different sets possible. "
                    + "Requested " + numSets + " — some questions will repeat across sets.";
        }
        return null;
    }

    public GeneratedSet loadPaper(Long paperId) {
        GeneratedPaper paper = paperRepo.findById(paperId)
                .orElseThrow(() -> new NoSuchElementException("Paper not found: " + paperId));

        List<PaperQuestion> mappings = paperQuestionRepo
                .findByPaperIdOrderByQuestionNumberAscChoiceLabelAsc(paperId);

        List<Question> sectionA = new ArrayList<>();
        Map<Integer, Question[]> pairMap = new TreeMap<>();

        for (PaperQuestion pq : mappings) {
            Question q = questionRepo.findById(pq.getQuestionId())
                    .orElseThrow(() -> new NoSuchElementException("Question not found: " + pq.getQuestionId()));

            if ("SECTION_A".equals(pq.getSection())) {
                sectionA.add(q);
            } else {
                Question[] pair = pairMap.computeIfAbsent(pq.getPairIndex(), k -> new Question[2]);
                pair["a".equals(pq.getChoiceLabel()) ? 0 : 1] = q;
            }
        }

        List<PairingEngine.QuestionPair> pairs = new ArrayList<>();
        for (Map.Entry<Integer, Question[]> entry : pairMap.entrySet()) {
            pairs.add(new PairingEngine.QuestionPair(
                    entry.getValue()[0], entry.getValue()[1],
                    entry.getValue()[0].getUnit(), entry.getKey()));
        }

        return new GeneratedSet(paper, sectionA, pairs);
    }

    @Transactional
    public void finalizePaper(Long paperId) {
        GeneratedPaper paper = paperRepo.findById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Paper not found"));
        paper.setIsFinal(true);
        paperRepo.save(paper);
    }

    public java.util.Optional<GeneratedPaper> getPaperById(Long paperId) {
        return paperRepo.findById(paperId);
    }

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

        List<Long> currentQuestionIds = paperQuestionRepo.findByPaperIdOrderByQuestionNumberAscChoiceLabelAsc(paperId)
                .stream().map(PaperQuestion::getQuestionId).collect(Collectors.toList());

        List<Question> alternatives = questionRepo.findBySessionIdOrderByUnitAscSerialNoAsc(paper.getSessionId())
                .stream()
                .filter(q -> q.getUnit() == oldQuestion.getUnit()
                        && q.getMarks() == oldQuestion.getMarks()
                        && q.getT() == oldQuestion.getT()
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
