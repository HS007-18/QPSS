package com.qpss.service;

import com.qpss.model.ExamConfig;
import com.qpss.model.Question;
import com.qpss.repository.ExamConfigRepository;
import com.qpss.repository.QuestionRepository;
import lombok.*;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SelectionEngine {

    private final QuestionRepository questionRepo;
    private final ExamConfigRepository configRepo;
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

    public SelectionResult select(String examType, Long subjectId, Long sessionId) {
        return select(examType, subjectId, sessionId, Collections.emptySet());
    }

    public SelectionResult select(String examType, Long subjectId, Long sessionId, Set<Long> excludeIds) {
        List<ExamConfig> rules = configRepo.findByExamTypeOrderByMarksAscUnitAsc(examType);
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("No exam config found for: " + examType);
        }

        List<Shortage> shortages = new ArrayList<>();
        List<Question> selected2m = new ArrayList<>();
        List<Question> selected16m = new ArrayList<>();

        Set<String> usedContents = new HashSet<>();

        for (ExamConfig rule : rules) {
            List<Question> pool = questionRepo
                    .findBySubjectIdAndSessionIdAndUnitAndMarks(subjectId, sessionId, rule.getUnit(), rule.getMarks())
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
                shortages.add(new Shortage(rule.getUnit(), rule.getMarks(), rule.getRequiredCount(), pool.size()));
                continue;
            }

            Collections.shuffle(pool, random);
            List<Question> picked = pool.subList(0, rule.getRequiredCount());

            for (Question q : picked) {
                usedContents.add(q.getQuestionContent().trim());
                if (rule.getMarks() == 2) {
                    selected2m.add(q);
                } else {
                    selected16m.add(q);
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
