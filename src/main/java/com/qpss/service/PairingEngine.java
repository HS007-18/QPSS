package com.qpss.service;

import com.qpss.model.Question;
import lombok.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PairingEngine {

    @Data @AllArgsConstructor
    public static class QuestionPair {
        private Question choiceA;
        private Question choiceB;
        private int unit;
        private int pairIndex;
    }

    public List<QuestionPair> createPairs(List<Question> sixteenMarkQuestions) {
        if (sixteenMarkQuestions.size() % 2 != 0) {
            throw new IllegalStateException(
                    "16M question count must be even, got: " + sixteenMarkQuestions.size());
        }

        Map<Integer, List<Question>> byUnit = sixteenMarkQuestions.stream()
                .collect(Collectors.groupingBy(Question::getUnit, TreeMap::new, Collectors.toList()));

        List<QuestionPair> pairs = new ArrayList<>();
        int pairIndex = 1;

        for (Map.Entry<Integer, List<Question>> entry : byUnit.entrySet()) {
            List<Question> unitQuestions = entry.getValue();

            if (unitQuestions.size() % 2 != 0) {
                throw new IllegalStateException(
                        "Unit " + entry.getKey() + " has odd 16M count: " + unitQuestions.size());
            }

            for (int i = 0; i < unitQuestions.size(); i += 2) {
                pairs.add(new QuestionPair(
                        unitQuestions.get(i),
                        unitQuestions.get(i + 1),
                        entry.getKey(),
                        pairIndex++
                ));
            }
        }

        return pairs;
    }
}
