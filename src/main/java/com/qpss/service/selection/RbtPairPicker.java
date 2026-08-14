package com.qpss.service.selection;

import com.qpss.model.Question;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RbtPairPicker {

    @Data
    public static class PickResult {
        private final List<Question> t1;
        private final List<Question> t2;
    }

    private final SecureRandom random = new SecureRandom();

    public List<Question> pickSameHalf(List<Question> pool, int required, Set<Long> selectedQuestionIds) {
        List<Question> distinct = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Question q : pool) {
            if (!selectedQuestionIds.contains(q.getId()) && seen.add(q.getId())) {
                distinct.add(q);
            }
        }

        java.util.Collections.shuffle(distinct, random);

        Map<String, List<Question>> byRbt = new LinkedHashMap<>();
        for (Question q : distinct) {
            byRbt.computeIfAbsent(rbtKey(q), k -> new ArrayList<>()).add(q);
        }

        List<Question> picked = new ArrayList<>();
        int requiredPairs = (required + 1) / 2;
        int pairs = 0;
        while (pairs < requiredPairs) {
            List<Question> takeFrom = null;
            for (List<Question> group : byRbt.values()) {
                if (group.size() >= 2 && (takeFrom == null || group.size() > takeFrom.size())) {
                    takeFrom = group;
                }
            }
            Question first;
            Question second;
            if (takeFrom != null) {
                first = takeFrom.remove(0);
                second = takeFrom.remove(0);
            } else {
                first = takeOne(byRbt);
                second = takeOne(byRbt);
                if (first == null || second == null) {
                    break;
                }
            }
            picked.add(first);
            picked.add(second);
            selectedQuestionIds.add(first.getId());
            selectedQuestionIds.add(second.getId());
            pairs++;
        }

        return picked;
    }

    public PickResult pickCrossHalf(List<Question> t1Pool, List<Question> t2Pool, int requiredT1, int requiredT2,
                                    Set<Long> selectedQuestionIds) {
        List<Question> t1Distinct = new ArrayList<>();
        List<Question> t2Distinct = new ArrayList<>();
        Set<Long> seenT1 = new HashSet<>();
        Set<Long> seenT2 = new HashSet<>();
        for (Question q : t1Pool) {
            if (!selectedQuestionIds.contains(q.getId()) && seenT1.add(q.getId())) t1Distinct.add(q);
        }
        for (Question q : t2Pool) {
            if (!selectedQuestionIds.contains(q.getId()) && seenT2.add(q.getId())) t2Distinct.add(q);
        }

        java.util.Collections.shuffle(t1Distinct, random);
        java.util.Collections.shuffle(t2Distinct, random);

        Map<String, List<Question>> t1ByRbt = new LinkedHashMap<>();
        Map<String, List<Question>> t2ByRbt = new LinkedHashMap<>();
        for (Question q : t1Distinct) t1ByRbt.computeIfAbsent(rbtKey(q), k -> new ArrayList<>()).add(q);
        for (Question q : t2Distinct) t2ByRbt.computeIfAbsent(rbtKey(q), k -> new ArrayList<>()).add(q);

        List<Question> pickedT1 = new ArrayList<>();
        List<Question> pickedT2 = new ArrayList<>();

        int pairsNeeded = Math.min(requiredT1, requiredT2);
        int pairs = 0;
        while (pairs < pairsNeeded) {
            String matchedRbt = null;
            for (Map.Entry<String, List<Question>> entry : t1ByRbt.entrySet()) {
                List<Question> t2Group = t2ByRbt.get(entry.getKey());
                if (!entry.getValue().isEmpty() && t2Group != null && !t2Group.isEmpty()) {
                    matchedRbt = entry.getKey();
                    break;
                }
            }
            Question t1Question;
            Question t2Question;
            if (matchedRbt != null) {
                t1Question = t1ByRbt.get(matchedRbt).remove(0);
                t2Question = t2ByRbt.get(matchedRbt).remove(0);
            } else {
                t1Question = takeOne(t1ByRbt);
                t2Question = takeOne(t2ByRbt);
                if (t1Question == null || t2Question == null) {
                    break;
                }
            }
            pickedT1.add(t1Question);
            pickedT2.add(t2Question);
            selectedQuestionIds.add(t1Question.getId());
            selectedQuestionIds.add(t2Question.getId());
            pairs++;
        }

        if (pickedT1.size() < requiredT1) {
            fillRemaining(t1Pool, requiredT1, selectedQuestionIds, pickedT1);
        }
        if (pickedT2.size() < requiredT2) {
            fillRemaining(t2Pool, requiredT2, selectedQuestionIds, pickedT2);
        }

        return new PickResult(pickedT1, pickedT2);
    }

    private Question takeOne(Map<String, List<Question>> byRbt) {
        Iterator<List<Question>> it = byRbt.values().iterator();
        while (it.hasNext()) {
            List<Question> group = it.next();
            if (group.isEmpty()) {
                it.remove();
                continue;
            }
            return group.remove(0);
        }
        return null;
    }

    private void fillRemaining(List<Question> pool, int required, Set<Long> selectedQuestionIds, List<Question> picked) {
        List<Question> candidates = new ArrayList<>(pool);
        java.util.Collections.shuffle(candidates, random);
        for (Question q : candidates) {
            if (picked.size() >= required) {
                break;
            }
            if (!selectedQuestionIds.contains(q.getId())) {
                selectedQuestionIds.add(q.getId());
                picked.add(q);
            }
        }
    }

    private String rbtKey(Question q) {
        return q.getRbt() == null ? "" : q.getRbt();
    }
}
