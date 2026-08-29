package com.qpss.domain.selection;
import com.qpss.entity.Question;
import lombok.Data;
import org.springframework.stereotype.Component;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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

    public List<Question> pickSameHalf(List<Question> pool, int required, Set<Long> selectedQuestionIds, Set<String> selectedQuestionContents) {
        List<Question> distinct = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (Question q : pool) {
            String contentKey = contentKey(q);
            if (!selectedQuestionIds.contains(q.getId()) && seen.add(q.getId())
                    && (!selectedQuestionContents.contains(contentKey) || contentKey.isEmpty())) {
                distinct.add(q);
            }
        }

        Collections.shuffle(distinct, random);

        Map<String, List<Question>> byKey = new LinkedHashMap<>();
        for (Question q : distinct) {
            byKey.computeIfAbsent(pairKey(q), k -> new ArrayList<>()).add(q);
        }

        List<Question> picked = new ArrayList<>();
        Set<String> pickedContents = new HashSet<>(selectedQuestionContents);

        int pairsNeeded = required / 2;
        int pairs = 0;
        while (pairs < pairsNeeded) {
            List<Question> takeFrom = null;
            for (List<Question> group : byKey.values()) {
                if (group.size() >= 2 && (takeFrom == null || group.size() > takeFrom.size())) {
                    takeFrom = group;
                }
            }
            if (takeFrom == null) {
                break;
            }
            Question q1 = removePick(takeFrom, pickedContents);
            Question q2 = removePick(takeFrom, pickedContents);
            if (q1 == null || q2 == null) {
                break;
            }
            picked.add(q1);
            picked.add(q2);
            selectedQuestionIds.add(q1.getId());
            selectedQuestionIds.add(q2.getId());
            pairs++;
        }

        for (Question q : picked) {
            String contentKey = contentKey(q);
            if (!contentKey.isEmpty()) {
                selectedQuestionContents.add(contentKey);
            }
        }

        if (picked.size() < required) {
            fillRemaining(distinct, required, selectedQuestionIds, selectedQuestionContents, picked);
        }

        return picked;
    }

    public PickResult pickCrossHalf(List<Question> t1Pool, List<Question> t2Pool, int requiredT1, int requiredT2,
                                    Set<Long> selectedQuestionIds, Set<String> selectedQuestionContents) {
        List<Question> t1Distinct = new ArrayList<>();
        List<Question> t2Distinct = new ArrayList<>();
        Set<Long> seenT1 = new HashSet<>();
        Set<Long> seenT2 = new HashSet<>();
        for (Question q : t1Pool) {
            String contentKey = contentKey(q);
            if (!selectedQuestionIds.contains(q.getId()) && seenT1.add(q.getId())
                    && (!selectedQuestionContents.contains(contentKey) || contentKey.isEmpty())) t1Distinct.add(q);
        }
        for (Question q : t2Pool) {
            String contentKey = contentKey(q);
            if (!selectedQuestionIds.contains(q.getId()) && seenT2.add(q.getId())
                    && (!selectedQuestionContents.contains(contentKey) || contentKey.isEmpty())) t2Distinct.add(q);
        }

        Collections.shuffle(t1Distinct, random);
        Collections.shuffle(t2Distinct, random);

        Map<String, List<Question>> t1ByKey = new LinkedHashMap<>();
        Map<String, List<Question>> t2ByKey = new LinkedHashMap<>();
        for (Question q : t1Distinct) t1ByKey.computeIfAbsent(pairKey(q), k -> new ArrayList<>()).add(q);
        for (Question q : t2Distinct) t2ByKey.computeIfAbsent(pairKey(q), k -> new ArrayList<>()).add(q);

        List<Question> pickedT1 = new ArrayList<>();
        List<Question> pickedT2 = new ArrayList<>();
        Set<String> pickedContents = new HashSet<>(selectedQuestionContents);

        int pairsNeeded = Math.min(requiredT1, requiredT2);
        int pairs = 0;
        while (pairs < pairsNeeded) {
            String matchedKey = null;
            for (Map.Entry<String, List<Question>> entry : t1ByKey.entrySet()) {
                List<Question> t2Group = t2ByKey.get(entry.getKey());
                if (!entry.getValue().isEmpty() && t2Group != null && !t2Group.isEmpty()) {
                    matchedKey = entry.getKey();
                    break;
                }
            }
            Question t1Question;
            Question t2Question;
            if (matchedKey != null) {
                t1Question = removePick(t1ByKey.get(matchedKey), pickedContents);
                t2Question = removePick(t2ByKey.get(matchedKey), pickedContents);
            } else {
                break;
            }
            if (t1Question == null || t2Question == null) {
                break;
            }
            pickedT1.add(t1Question);
            pickedT2.add(t2Question);
            selectedQuestionIds.add(t1Question.getId());
            selectedQuestionIds.add(t2Question.getId());
            pairs++;
        }

        for (Question q : pickedT1) {
            String contentKey = contentKey(q);
            if (!contentKey.isEmpty()) {
                selectedQuestionContents.add(contentKey);
            }
        }
        for (Question q : pickedT2) {
            String contentKey = contentKey(q);
            if (!contentKey.isEmpty()) {
                selectedQuestionContents.add(contentKey);
            }
        }

        if (pickedT1.size() < requiredT1) {
            fillRemaining(t1Pool, requiredT1, selectedQuestionIds, selectedQuestionContents, pickedT1);
        }
        if (pickedT2.size() < requiredT2) {
            fillRemaining(t2Pool, requiredT2, selectedQuestionIds, selectedQuestionContents, pickedT2);
        }

        return new PickResult(pickedT1, pickedT2);
    }

    private Question removePick(List<Question> group, Set<String> pickedContents) {
        for (int i = 0; i < group.size(); i++) {
            Question q = group.get(i);
            String contentKey = contentKey(q);
            if (contentKey.isEmpty() || pickedContents.add(contentKey)) {
                return group.remove(i);
            }
        }
        return null;
    }

    private void fillRemaining(List<Question> pool, int required, Set<Long> selectedQuestionIds, Set<String> selectedQuestionContents, List<Question> picked) {
        List<Question> candidates = new ArrayList<>(pool);
        Collections.shuffle(candidates, random);
        for (Question q : candidates) {
            if (picked.size() >= required) {
                break;
            }
            String contentKey = contentKey(q);
            if (!selectedQuestionIds.contains(q.getId()) && (!selectedQuestionContents.contains(contentKey) || contentKey.isEmpty())) {
                selectedQuestionIds.add(q.getId());
                if (!contentKey.isEmpty()) {
                    selectedQuestionContents.add(contentKey);
                }
                picked.add(q);
            }
        }
    }

    private String contentKey(Question q) {
        return q.getQuestionContent() != null ? q.getQuestionContent().trim().toLowerCase() : "";
    }

    private String pairKey(Question q) {
        String rbt = q.getRbt() == null ? "" : q.getRbt();
        String type = q.getQuestionType() == null ? "" : q.getQuestionType();
        return rbt + "_" + type;
    }
}