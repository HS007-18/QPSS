package com.qpss.generation.selection;

import com.qpss.questionbank.model.Question;
import com.qpss.questionbank.repository.QuestionRepository;
import com.qpss.generation.distribution.DistributionPlan;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SelectionEngine {

    private final QuestionRepository questionRepo;
    private final RbtPairPicker pairPicker;
    private SecureRandom random = new SecureRandom();

    @Data
    @AllArgsConstructor
    public static class SelectionBucket {
        private int marks;
        private int unit;
        private int t;
        private int required;
    }

    @Data
    @AllArgsConstructor
    public static class SelectedBucket {
        private int marks;
        private int unit;
        private int t;
        private List<Question> questions;
    }

    @Data
    @AllArgsConstructor
    public static class SelectionShortage {
        private int unit;
        private int marks;
        private int t;
        private int required;
        private int available;
        private int shortage;
    }

    @Data
    public static class SelectionResult {
        private final boolean successful;
        private final List<SelectedBucket> selectedBuckets;
        private final List<SelectionShortage> shortages;

        public static SelectionResult success(List<SelectedBucket> buckets) {
            return new SelectionResult(true, buckets, Collections.emptyList());
        }

        public static SelectionResult failure(List<SelectionShortage> shortages) {
            return new SelectionResult(false, Collections.emptyList(), shortages);
        }

        public List<Question> getQuestionsByMarks(int marks) {
            return selectedBuckets.stream()
                    .filter(b -> b.getMarks() == marks)
                    .flatMap(b -> b.getQuestions().stream())
                    .sorted(Comparator.comparingInt(Question::getUnit))
                    .collect(Collectors.toList());
        }

        public List<Question> getTwoMarkQuestions() {
            return getQuestionsByMarks(2);
        }

        public List<Question> getPartBQuestions(int partBMarks) {
            return getQuestionsByMarks(partBMarks);
        }
    }

    public SelectionResult select(DistributionPlan plan, Long subjectId, Long sessionId) {
        return select(plan, subjectId, sessionId, Collections.emptySet(), null);
    }

    public SelectionResult select(DistributionPlan plan, Long subjectId, Long sessionId, Set<Long> excludeIds) {
        return select(plan, subjectId, sessionId, excludeIds, null);
    }

    public SelectionResult select(DistributionPlan plan, Long subjectId, Long sessionId, Set<Long> excludeIds,
                                  PairingEngine.PairingMode pairingMode) {
        validateInput(plan, subjectId, sessionId);

        List<SelectionBucket> buckets = flattenPlan(plan);

        List<SelectionShortage> shortages = checkAvailability(buckets, subjectId, sessionId, excludeIds);
        if (!shortages.isEmpty()) {
            return SelectionResult.failure(shortages);
        }

        List<SelectedBucket> selectedBuckets = performSelection(buckets, subjectId, sessionId, excludeIds, pairingMode);
        if (selectedBuckets == null) {
            shortages = checkDistinctContentAvailability(buckets, subjectId, sessionId, excludeIds);
            return SelectionResult.failure(shortages);
        }

        validateConsistency(plan, selectedBuckets);

        return SelectionResult.success(selectedBuckets);
    }

    private void validateInput(DistributionPlan plan, Long subjectId, Long sessionId) {
        if (plan == null || plan.getSections() == null || plan.getSections().isEmpty()) {
            throw new IllegalArgumentException("Invalid or empty distribution plan");
        }
        if (subjectId == null) {
            throw new IllegalArgumentException("Subject ID is required");
        }
        if (sessionId == null) {
            throw new IllegalArgumentException("Session ID is required");
        }
    }

    private List<SelectionBucket> flattenPlan(DistributionPlan plan) {
        List<SelectionBucket> buckets = new ArrayList<>();
        for (DistributionPlan.SectionPlan section : plan.getSections()) {
            for (DistributionPlan.UnitPlan rule : section.getUnits()) {
                if (rule.getT1Required() > 0) {
                    buckets.add(new SelectionBucket(section.getMarks(), rule.getUnit(), 1, rule.getT1Required()));
                }
                if (rule.getT2Required() > 0) {
                    buckets.add(new SelectionBucket(section.getMarks(), rule.getUnit(), 2, rule.getT2Required()));
                }
            }
        }
        return buckets;
    }

    private List<SelectionShortage> checkAvailability(List<SelectionBucket> buckets, Long subjectId, Long sessionId,
                                                      Set<Long> excludeIds) {
        List<SelectionShortage> shortages = new ArrayList<>();

        Set<Long> seenIds = new HashSet<>(excludeIds);

        for (SelectionBucket bucket : buckets) {
            List<Question> candidates = questionRepo.findBySubjectIdAndSessionIdAndUnitAndMarksAndT(
                    subjectId, sessionId, bucket.getUnit(), bucket.getMarks(), bucket.getT());

            long uniqueAvailable = distinctAvailableCount(candidates, seenIds);

            if (uniqueAvailable < bucket.getRequired()) {
                shortages.add(new SelectionShortage(
                        bucket.getUnit(), bucket.getMarks(), bucket.getT(),
                        bucket.getRequired(), (int) uniqueAvailable,
                        bucket.getRequired() - (int) uniqueAvailable));
            } else {
                int reserved = 0;
                for (Question q : candidates) {
                    if (reserved >= bucket.getRequired()) {
                        break;
                    }
                    if (seenIds.add(q.getId())) {
                        reserved++;
                    }
                }
            }
        }
        return shortages;
    }

    private long distinctAvailableCount(List<Question> candidates, Set<Long> seenIds) {
        return candidates.stream()
                .map(Question::getId)
                .filter(id -> !seenIds.contains(id))
                .distinct()
                .count();
    }

    private List<SelectionShortage> checkDistinctContentAvailability(List<SelectionBucket> buckets, Long subjectId,
                                                                     Long sessionId, Set<Long> excludeIds) {
        List<SelectionShortage> shortages = new ArrayList<>();
        for (SelectionBucket bucket : buckets) {
            List<Question> candidates = questionRepo.findBySubjectIdAndSessionIdAndUnitAndMarksAndT(
                    subjectId, sessionId, bucket.getUnit(), bucket.getMarks(), bucket.getT());
            Set<String> seenContents = new HashSet<>();
            long uniqueAvailable = 0;
            for (Question q : candidates) {
                if (excludeIds.contains(q.getId())) {
                    continue;
                }
                String contentKey = q.getQuestionContent() != null ? q.getQuestionContent().trim().toLowerCase() : "";
                if (seenContents.add(contentKey) || contentKey.isEmpty()) {
                    uniqueAvailable++;
                }
            }
            if (uniqueAvailable < bucket.getRequired()) {
                shortages.add(new SelectionShortage(
                        bucket.getUnit(), bucket.getMarks(), bucket.getT(),
                        bucket.getRequired(), (int) uniqueAvailable,
                        bucket.getRequired() - (int) uniqueAvailable));
            }
        }
        return shortages;
    }

    private List<SelectedBucket> performSelection(List<SelectionBucket> buckets, Long subjectId, Long sessionId,
                                                  Set<Long> excludeIds, PairingEngine.PairingMode pairingMode) {
        List<SelectedBucket> selectedBuckets = new ArrayList<>();
        Set<Long> selectedQuestionIds = new HashSet<>(excludeIds);
        Set<String> selectedQuestionContents = new HashSet<>();

        if (!excludeIds.isEmpty()) {
            for (Long id : excludeIds) {
                questionRepo.findById(id).ifPresent(q -> {
                    if (q.getQuestionContent() != null) {
                        selectedQuestionContents.add(q.getQuestionContent().trim().toLowerCase());
                    }
                });
            }
        }

        for (int bi = 0; bi < buckets.size(); bi++) {
            SelectionBucket bucket = buckets.get(bi);
            boolean partB = bucket.getMarks() == 16 || bucket.getMarks() == 20;

            if (pairingMode == PairingEngine.PairingMode.CROSS_HALF && partB && bucket.getT() == 1
                    && bi + 1 < buckets.size()) {
                SelectionBucket t2Bucket = buckets.get(bi + 1);
                if (t2Bucket.getMarks() == bucket.getMarks() && t2Bucket.getUnit() == bucket.getUnit()
                        && t2Bucket.getT() == 2) {
                    bi++;
                    List<Question> t1Pool = questionRepo.findBySubjectIdAndSessionIdAndUnitAndMarksAndT(
                            subjectId, sessionId, bucket.getUnit(), bucket.getMarks(), 1);
                    List<Question> t2Pool = questionRepo.findBySubjectIdAndSessionIdAndUnitAndMarksAndT(
                            subjectId, sessionId, t2Bucket.getUnit(), t2Bucket.getMarks(), 2);
                    RbtPairPicker.PickResult pick = pairPicker.pickCrossHalf(
                            t1Pool, t2Pool, bucket.getRequired(), t2Bucket.getRequired(), selectedQuestionIds, selectedQuestionContents);
                    if (pick.getT1().size() < bucket.getRequired() || pick.getT2().size() < t2Bucket.getRequired()) {
                        return null;
                    }
                    for (Question q : pick.getT1()) {
                        if (q.getQuestionContent() != null) selectedQuestionContents.add(q.getQuestionContent().trim().toLowerCase());
                    }
                    for (Question q : pick.getT2()) {
                        if (q.getQuestionContent() != null) selectedQuestionContents.add(q.getQuestionContent().trim().toLowerCase());
                    }
                    
                    selectedBuckets.add(new SelectedBucket(bucket.getMarks(), bucket.getUnit(), 1, pick.getT1()));
                    selectedBuckets.add(new SelectedBucket(t2Bucket.getMarks(), t2Bucket.getUnit(), 2, pick.getT2()));
                    continue;
                }
            }

            List<Question> pool = questionRepo.findBySubjectIdAndSessionIdAndUnitAndMarksAndT(
                    subjectId, sessionId, bucket.getUnit(), bucket.getMarks(), bucket.getT());

            if (pairingMode == PairingEngine.PairingMode.SAME_HALF && partB) {
                List<Question> picked = pairPicker.pickSameHalf(pool, bucket.getRequired(), selectedQuestionIds, selectedQuestionContents);
                if (picked.size() < bucket.getRequired()) {
                    return null;
                }
                for (Question q : picked) {
                    if (q.getQuestionContent() != null) selectedQuestionContents.add(q.getQuestionContent().trim().toLowerCase());
                }
                selectedBuckets.add(new SelectedBucket(bucket.getMarks(), bucket.getUnit(), bucket.getT(), picked));
                continue;
            }

            Collections.shuffle(pool, random);

            List<Question> picked = new ArrayList<>();
            for (Question q : pool) {
                if (picked.size() >= bucket.getRequired()) {
                    break;
                }
                String contentKey = q.getQuestionContent() != null ? q.getQuestionContent().trim().toLowerCase() : "";
                if (!selectedQuestionIds.contains(q.getId()) && (!selectedQuestionContents.contains(contentKey) || contentKey.isEmpty())) {
                    selectedQuestionIds.add(q.getId());
                    if (!contentKey.isEmpty()) {
                        selectedQuestionContents.add(contentKey);
                    }
                    picked.add(q);
                }
            }

            if (picked.size() < bucket.getRequired()) {
                return null;
            }

            selectedBuckets.add(new SelectedBucket(bucket.getMarks(), bucket.getUnit(), bucket.getT(), picked));
        }

        return selectedBuckets;
    }

    private void validateConsistency(DistributionPlan plan, List<SelectedBucket> selectedBuckets) {
        Set<Long> uniqueIds = new HashSet<>();


        for (SelectedBucket bucket : selectedBuckets) {
            if (bucket.getQuestions().size() != bucket.getQuestions().stream().map(Question::getId).distinct().count()) {
                throw new IllegalStateException("Duplicate question IDs within bucket");
            }
            for (Question q : bucket.getQuestions()) {
                if (!uniqueIds.add(q.getId())) {
                    throw new IllegalStateException("Duplicate question ID across buckets: " + q.getId());
                }
                if (q.getMarks() != bucket.getMarks() || q.getUnit() != bucket.getUnit() || q.getT() != bucket.getT()) {
                    throw new IllegalStateException("Question properties do not match bucket constraints");
                }
            }
        }

        for (DistributionPlan.SectionPlan section : plan.getSections()) {
            long sectionSelected = selectedBuckets.stream()
                    .filter(b -> b.getMarks() == section.getMarks())
                    .flatMap(b -> b.getQuestions().stream())
                    .count();
            if (sectionSelected != section.getTotalRequired()) {
                throw new IllegalStateException(String.format("Section %dM mismatch: selected=%d, required=%d",
                        section.getMarks(), sectionSelected, section.getTotalRequired()));
            }

            for (DistributionPlan.UnitPlan unitPlan : section.getUnits()) {
                long unitT1Selected = selectedBuckets.stream()
                        .filter(b -> b.getMarks() == section.getMarks() && b.getUnit() == unitPlan.getUnit() && b.getT() == 1)
                        .flatMap(b -> b.getQuestions().stream())
                        .count();
                if (unitT1Selected != unitPlan.getT1Required()) {
                    throw new IllegalStateException(String.format("Unit %d T1 mismatch: selected=%d, required=%d",
                            unitPlan.getUnit(), unitT1Selected, unitPlan.getT1Required()));
                }

                long unitT2Selected = selectedBuckets.stream()
                        .filter(b -> b.getMarks() == section.getMarks() && b.getUnit() == unitPlan.getUnit() && b.getT() == 2)
                        .flatMap(b -> b.getQuestions().stream())
                        .count();
                if (unitT2Selected != unitPlan.getT2Required()) {
                    throw new IllegalStateException(String.format("Unit %d T2 mismatch: selected=%d, required=%d",
                            unitPlan.getUnit(), unitT2Selected, unitPlan.getT2Required()));
                }

                long totalUnitSelected = unitT1Selected + unitT2Selected;
                if (totalUnitSelected != unitPlan.getRequiredCount()) {
                    throw new IllegalStateException(String.format("Unit %d mismatch: selected=%d, required=%d",
                            unitPlan.getUnit(), totalUnitSelected, unitPlan.getRequiredCount()));
                }
            }
        }
    }
}
