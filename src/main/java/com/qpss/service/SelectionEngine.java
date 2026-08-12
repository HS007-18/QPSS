package com.qpss.service;

import com.qpss.model.Question;
import com.qpss.repository.QuestionRepository;
import com.qpss.service.distribution.DistributionPlan;
import lombok.*;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SelectionEngine {

    private final QuestionRepository questionRepo;
    private final SecureRandom random = new SecureRandom();

    @Data @AllArgsConstructor
    public static class Shortage {
        private int unit;
        private int marks;
        private int required;
        private int available;
    }

    @Data @AllArgsConstructor
    public static class SelectionResult {
        private List<Question> twoMarkQuestions;
        private List<Question> sixteenMarkQuestions;
        private List<Shortage> shortages;
        private boolean successful;

        public static SelectionResult success(List<Question> twoMark, List<Question> sixteenMark) {
            return new SelectionResult(twoMark, sixteenMark, Collections.emptyList(), true);
        }

        public static SelectionResult failure(List<Shortage> shortages) {
            return new SelectionResult(Collections.emptyList(), Collections.emptyList(), shortages, false);
        }
    }

    public SelectionResult select(DistributionPlan plan, Long subjectId, Long sessionId) {
        return select(plan, subjectId, sessionId, Collections.emptySet());
    }

    public SelectionResult select(DistributionPlan plan, Long subjectId, Long sessionId, Set<Long> excludeIds) {
        if (plan == null || plan.getSections().isEmpty()) {
            throw new IllegalArgumentException("Invalid or empty distribution plan");
        }

        List<Shortage> shortages = new ArrayList<>();
        List<Question> selected2m = new ArrayList<>();
        List<Question> selected16m = new ArrayList<>();

        Set<String> usedContents = new HashSet<>();

        for (DistributionPlan.SectionPlan section : plan.getSections()) {
            for (DistributionPlan.UnitPlan rule : section.getUnits()) {
                List<Question> pool = questionRepo
                        .findBySubjectIdAndSessionIdAndUnitAndMarks(subjectId, sessionId, rule.getUnit(), section.getMarks())
                        .stream()
                        .filter(q -> !excludeIds.contains(q.getId()))
                        .collect(Collectors.toList());

                // Remove duplicates from pool and avoid contents already used in this paper
                Set<String> localSeen = new HashSet<>();
                pool.removeIf(q -> {
                    String content = q.getQuestionContent().trim();
                    return usedContents.contains(content) || !localSeen.add(content);
                });

                if (pool.size() < rule.getRequiredCount()) {
                    shortages.add(new Shortage(rule.getUnit(), section.getMarks(), rule.getRequiredCount(), pool.size()));
                    continue;
                }

                // Currently just doing basic randomized selection logic from original code, but satisfying T1/T2 required counts
                List<Question> t1Pool = pool.stream().filter(q -> q.getT() == 1).collect(Collectors.toList());
                List<Question> t2Pool = pool.stream().filter(q -> q.getT() == 2).collect(Collectors.toList());

                Collections.shuffle(t1Pool, random);
                Collections.shuffle(t2Pool, random);

                if (t1Pool.size() < rule.getT1Required() || t2Pool.size() < rule.getT2Required()) {
                     shortages.add(new Shortage(rule.getUnit(), section.getMarks(), rule.getRequiredCount(), pool.size()));
                     continue;
                }

                List<Question> picked = new ArrayList<>();
                picked.addAll(t1Pool.subList(0, rule.getT1Required()));
                picked.addAll(t2Pool.subList(0, rule.getT2Required()));

                for (Question q : picked) {
                    usedContents.add(q.getQuestionContent().trim());
                    if (section.getMarks() == 2) {
                        selected2m.add(q);
                    } else {
                        selected16m.add(q);
                    }
                }
            }
        }

        if (!shortages.isEmpty()) {
            return SelectionResult.failure(shortages);
        }

        selected2m.sort(Comparator.comparingInt(Question::getUnit));
        selected16m.sort(Comparator.comparingInt(Question::getUnit));

        return SelectionResult.success(new ArrayList<>(selected2m), new ArrayList<>(selected16m));
    }
}
