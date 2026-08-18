package com.qpss.generation.selection;

import com.qpss.questionbank.model.Question;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class PairingEngine {

    public enum PairingMode { SAME_HALF, CROSS_HALF }

    @Data @AllArgsConstructor
    public static class QuestionPair {
        private Question choiceA;
        private Question choiceB;
        private int unit;
        private int pairIndex;
    }

    public List<QuestionPair> createPairs(List<Question> partBQuestions) {
        return createPairs(partBQuestions, PairingMode.SAME_HALF);
    }

    public List<QuestionPair> createPairs(List<Question> partBQuestions, PairingMode mode) {
        if (partBQuestions.size() % 2 != 0) {
            throw new IllegalStateException(
                    "Part B question count must be even, got: " + partBQuestions.size());
        }
        return mode == PairingMode.CROSS_HALF
                ? createCrossHalfPairs(partBQuestions)
                : createSameHalfPairs(partBQuestions);
    }

    private List<QuestionPair> createSameHalfPairs(List<Question> partBQuestions) {
        Map<String, List<Question>> byUnitAndT = partBQuestions.stream()
                .collect(Collectors.groupingBy(
                        q -> q.getUnit() + ":" + q.getT(),
                        () -> new TreeMap<>(Comparator
                                .comparing((String k) -> Integer.parseInt(k.split(":")[0]))
                                .thenComparing(k -> Integer.parseInt(k.split(":")[1]))),
                        Collectors.toList()));

        List<QuestionPair> pairs = new ArrayList<>();
        List<Question> spillover = new ArrayList<>();
        int pairIndex = 1;

        for (Map.Entry<String, List<Question>> entry : byUnitAndT.entrySet()) {
            List<Question> halfQuestions = entry.getValue();
            int unit = Integer.parseInt(entry.getKey().split(":")[0]);

            for (int i = 0; i + 1 < halfQuestions.size(); i += 2) {
                pairs.add(new QuestionPair(
                        halfQuestions.get(i),
                        halfQuestions.get(i + 1),
                        unit,
                        pairIndex++
                ));
            }
            if (halfQuestions.size() % 2 != 0) {
                spillover.add(halfQuestions.get(halfQuestions.size() - 1));
            }
        }

        if (spillover.size() % 2 != 0) {
            throw new IllegalStateException("Part B has odd leftover count across halves: " + spillover.size());
        }
        for (int i = 0; i < spillover.size(); i += 2) {
            pairs.add(new QuestionPair(
                    spillover.get(i),
                    spillover.get(i + 1),
                    spillover.get(i).getUnit(),
                    pairIndex++
            ));
        }

        return pairs;
    }

    private List<QuestionPair> createCrossHalfPairs(List<Question> partBQuestions) {
        Map<Integer, List<Question>> byUnit = partBQuestions.stream()
                .collect(Collectors.groupingBy(Question::getUnit, TreeMap::new, Collectors.toList()));

        List<QuestionPair> pairs = new ArrayList<>();
        int pairIndex = 1;

        for (Map.Entry<Integer, List<Question>> entry : byUnit.entrySet()) {
            List<Question> unitQuestions = entry.getValue();
            List<Question> t1Questions = new ArrayList<>();
            List<Question> t2Questions = new ArrayList<>();
            for (Question q : unitQuestions) {
                if (q.getT() == 1) {
                    t1Questions.add(q);
                } else {
                    t2Questions.add(q);
                }
            }
            if (t1Questions.size() != t2Questions.size()) {
                throw new IllegalStateException("Unit " + entry.getKey()
                        + " has unequal T1/T2 Part B counts: " + t1Questions.size() + "/" + t2Questions.size());
            }
            for (int i = 0; i < t1Questions.size(); i++) {
                pairs.add(new QuestionPair(
                        t1Questions.get(i),
                        t2Questions.get(i),
                        entry.getKey(),
                        pairIndex++
                ));
            }
        }

        return pairs;
    }
}
