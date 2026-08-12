package com.qpss.service;

import com.qpss.model.ExamConfig;
import com.qpss.model.Question;
import com.qpss.repository.ExamCoRuleRepository;
import com.qpss.repository.ExamConfigRepository;
import lombok.*;
import org.springframework.stereotype.Service;

import java.util.*;
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
            List<Question> twoMark,
            List<PairingEngine.QuestionPair> pairs) {

        List<String> failures = new ArrayList<>();
        com.qpss.service.distribution.DistributionPlan plan = examConfigService.getDistributionPlan(examType);
        Set<String> requiredCOs = coRuleRepo.findByExamType(examType).stream()
                .map(r -> r.getCo())
                .collect(Collectors.toSet());

        if (twoMark.size() != 10) {
            failures.add("Section A must have 10 questions, got " + twoMark.size());
        }

        if (pairs.size() != 5) {
            failures.add("Section B must have 5 A/B pairs, got " + pairs.size());
        }

        Set<Long> allIds = new HashSet<>();
        List<Question> allQuestions = new ArrayList<>(twoMark);

        for (Question q : twoMark) {
            if (q.getMarks() != 2) {
                failures.add("Section A Q" + q.getId() + " has marks=" + q.getMarks());
            }
            if (!q.getSubjectId().equals(subjectId)) {
                failures.add("Q" + q.getId() + " wrong subject");
            }
            if (!q.getSessionId().equals(sessionId)) {
                failures.add("Q" + q.getId() + " wrong session");
            }
            if (!allIds.add(q.getId())) {
                failures.add("Duplicate question ID: " + q.getId());
            }
        }

        for (PairingEngine.QuestionPair pair : pairs) {
            Question a = pair.getChoiceA();
            Question b = pair.getChoiceB();

            if (a.getMarks() != 16) failures.add("Pair " + pair.getPairIndex() + " choice A not 16M");
            if (b.getMarks() != 16) failures.add("Pair " + pair.getPairIndex() + " choice B not 16M");

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

        Map<String, Map<Integer, Long>> distribution = new HashMap<>();
        for (Question q : allQuestions) {
            String markKey = q.getMarks() + "M";
            distribution.computeIfAbsent(markKey, k -> new HashMap<>())
                    .merge(q.getUnit(), 1L, Long::sum);
        }

        for (com.qpss.service.distribution.DistributionPlan.SectionPlan sp : plan.getSections()) {
            String markKey = sp.getMarks() + "M";
            for (com.qpss.service.distribution.DistributionPlan.UnitPlan up : sp.getUnits()) {
                long actual = distribution.getOrDefault(markKey, Collections.emptyMap())
                        .getOrDefault(up.getUnit(), 0L);
                if (actual != up.getRequiredCount()) {
                    failures.add("Unit " + up.getUnit() + " " + markKey
                            + ": expected " + up.getRequiredCount() + ", got " + actual);
                }
            }
        }

        int pairIdx = 0;
        int prevUnit = 0;
        for (PairingEngine.QuestionPair pair : pairs) {
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
