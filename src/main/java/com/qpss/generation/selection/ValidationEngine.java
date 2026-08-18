package com.qpss.generation.selection;

import com.qpss.examconfig.model.ExamConfig;
import com.qpss.questionbank.model.Question;
import com.qpss.generation.distribution.DistributionPlan;
import com.qpss.examconfig.repository.ExamCoRuleRepository;
import com.qpss.examconfig.repository.ExamConfigRepository;
import com.qpss.examconfig.service.ExamConfigService;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ValidationEngine {

    private final ExamConfigService examConfigService;
    private final ExamCoRuleRepository coRuleRepo;

    @Data @Builder
    public static class ValidationResult {
        private boolean valid;
        @Builder.Default
        private List<String> failures = new ArrayList<>();

        public static ValidationResult pass() {
            return ValidationResult.builder().valid(true).build();
        }

        public static ValidationResult fail(List<String> failures) {
            return ValidationResult.builder().valid(false).failures(failures).build();
        }
    }

    public ValidationResult validate(
            String examType,
            Long subjectId,
            Long sessionId,
            String format,
            List<Question> sectionA,
            List<PairingEngine.QuestionPair> sectionBPairs) {

        List<String> failures = new ArrayList<>();
        DistributionPlan plan = examConfigService.getDistributionPlan(examType, format);
        Set<String> requiredCOs = coRuleRepo.findByExamType(examType).stream()
                .map(r -> r.getCo())
                .collect(Collectors.toSet());

        long expectedSectionA = plan.getSections().stream()
                .filter(s -> s.getMarks() == 2).findFirst().map(s -> (long) s.getTotalRequired()).orElse(0L);
        long expectedSectionBPairs = plan.getSections().stream()
                .filter(s -> s.getMarks() == 16 || s.getMarks() == 20).findFirst().map(s -> (long) s.getTotalRequired() / 2).orElse(0L);

        if (sectionA.size() != expectedSectionA) {
            failures.add("Section A must have " + expectedSectionA + " questions, got " + sectionA.size());
        }

        if (sectionBPairs.size() != expectedSectionBPairs) {
            failures.add("Section B must have " + expectedSectionBPairs + " A/B pairs, got " + sectionBPairs.size());
        }

        Set<Long> allIds = new HashSet<>();
        List<Question> allQuestions = new ArrayList<>(sectionA);

        for (Question q : sectionA) {
            if (q.getMarks() != 2) {
                failures.add("Section A Q" + q.getId() + " has marks=" + q.getMarks());
            }
            if (!q.getSubjectId().equals(subjectId)) failures.add("Q" + q.getId() + " wrong subject");
            if (!q.getSessionId().equals(sessionId)) failures.add("Q" + q.getId() + " wrong session");
            if (!allIds.add(q.getId())) failures.add("Duplicate question ID: " + q.getId());
        }

        for (PairingEngine.QuestionPair pair : sectionBPairs) {
            Question a = pair.getChoiceA();
            Question b = pair.getChoiceB();

            if (a.getMarks() != 16 && a.getMarks() != 20) failures.add("Pair " + pair.getPairIndex() + " choice A not 16M/20M");
            if (b.getMarks() != 16 && b.getMarks() != 20) failures.add("Pair " + pair.getPairIndex() + " choice B not 16M/20M");
            if (a.getUnit() != b.getUnit()) {
                failures.add("Pair " + pair.getPairIndex() + " mixed units: " + a.getUnit() + "/" + b.getUnit());
            }

            for (Question q : List.of(a, b)) {
                if (!q.getSubjectId().equals(subjectId)) failures.add("Q" + q.getId() + " wrong subject");
                if (!q.getSessionId().equals(sessionId)) failures.add("Q" + q.getId() + " wrong session");
                if (!allIds.add(q.getId())) failures.add("Duplicate question ID: " + q.getId());
                allQuestions.add(q);
            }
        }

        Map<String, Map<Integer, Map<Integer, Long>>> distribution = new HashMap<>();
        for (Question q : allQuestions) {
            String markKey = q.getMarks() + "M";
            distribution
                .computeIfAbsent(markKey, k -> new HashMap<>())
                .computeIfAbsent(q.getUnit(), k -> new HashMap<>())
                .merge(q.getT(), 1L, Long::sum);
        }

        for (DistributionPlan.SectionPlan sp : plan.getSections()) {
            String markKey = sp.getMarks() + "M";
            for (DistributionPlan.UnitPlan up : sp.getUnits()) {
                Map<Integer, Long> unitDist = distribution.getOrDefault(markKey, Collections.emptyMap())
                        .getOrDefault(up.getUnit(), Collections.emptyMap());
                long actualT1 = unitDist.getOrDefault(1, 0L);
                long actualT2 = unitDist.getOrDefault(2, 0L);
                long actualTotal = actualT1 + actualT2;

                if (actualTotal != up.getRequiredCount()) {
                    failures.add("Unit " + up.getUnit() + " " + markKey
                            + ": expected " + up.getRequiredCount() + " total, got " + actualTotal);
                }
                if (actualT1 != up.getT1Required()) {
                    failures.add("Unit " + up.getUnit() + " " + markKey
                            + ": expected " + up.getT1Required() + " T1, got " + actualT1);
                }
                if (actualT2 != up.getT2Required()) {
                    failures.add("Unit " + up.getUnit() + " " + markKey
                            + ": expected " + up.getT2Required() + " T2, got " + actualT2);
                }
            }
        }

        int pairIdx = 0;
        int prevUnit = 0;
        for (PairingEngine.QuestionPair pair : sectionBPairs) {
            if (pair.getUnit() < prevUnit) {
                failures.add("Pair " + pair.getPairIndex() + " breaks unit sequence");
            }
            prevUnit = pair.getUnit();
        }

        Set<String> foundCOs = allQuestions.stream()
                .map(Question::getCo)
                .collect(Collectors.toSet());
        for (String co : requiredCOs) {
            if (!foundCOs.contains(co)) {
                failures.add("Missing CO coverage: " + co);
            }
        }

        return failures.isEmpty() ? ValidationResult.pass() : ValidationResult.fail(failures);
    }
}
