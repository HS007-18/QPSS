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
    public static class SelectionBucket {
        private int marks;
        private int unit;
        private int t;
        private int required;
    }

    @Data @AllArgsConstructor
    public static class SelectedBucket {
        private int marks;
        private int unit;
        private int t;
        private List<Question> questions;
    }

    @Data @AllArgsConstructor
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

        public List<Question> getSixteenMarkQuestions() {
            return getQuestionsByMarks(16);
        }

        public List<Question> getPartBQuestions(int partBMarks) {
            return getQuestionsByMarks(partBMarks);
        }
    }

    public SelectionResult select(DistributionPlan plan, Long subjectId, Long sessionId) {
        return select(plan, subjectId, sessionId, Collections.emptySet());
    }

    public SelectionResult select(DistributionPlan plan, Long subjectId, Long sessionId, Set<Long> excludeIds) {
        validateInput(plan, subjectId, sessionId);

        List<SelectionBucket> buckets = flattenPlan(plan);

        // Complete availability check
        List<SelectionShortage> shortages = checkAvailability(buckets, subjectId, sessionId, excludeIds);
        if (!shortages.isEmpty()) {
            return SelectionResult.failure(shortages);
        }

        // Random Selection
        List<SelectedBucket> selectedBuckets = performSelection(buckets, subjectId, sessionId, excludeIds);
        if (selectedBuckets == null) {
            // Selection failed due to deduplication issues creating a shortage at selection time
            // which couldn't be detected during initial count.
            // Recalculate strict availability using distinct IDs to generate precise shortages.
            shortages = checkStrictAvailability(buckets, subjectId, sessionId, excludeIds);
            return SelectionResult.failure(shortages);
        }

        // Consistency check
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

    private List<SelectionShortage> checkAvailability(List<SelectionBucket> buckets, Long subjectId, Long sessionId, Set<Long> excludeIds) {
        List<SelectionShortage> shortages = new ArrayList<>();
        
        Set<Long> seenIds = new HashSet<>(excludeIds);

        for (SelectionBucket bucket : buckets) {
            List<Question> candidates = questionRepo.findBySubjectIdAndSessionIdAndUnitAndMarksAndT(
                    subjectId, sessionId, bucket.getUnit(), bucket.getMarks(), bucket.getT());

            long uniqueAvailable = candidates.stream()
                    .map(Question::getId)
                    .filter(id -> !seenIds.contains(id))
                    .distinct()
                    .count();

            if (uniqueAvailable < bucket.getRequired()) {
                shortages.add(new SelectionShortage(
                        bucket.getUnit(), bucket.getMarks(), bucket.getT(),
                        bucket.getRequired(), (int) uniqueAvailable,
                        bucket.getRequired() - (int) uniqueAvailable));
            } else {
                // To accurately predict shortages, we might need to simulate consuming IDs, 
                // but since buckets are strictly segregated by (Unit, Marks, T), 
                // candidates from one bucket will not overlap with candidates from another bucket
                // UNLESS there are duplicates in the DB for the exact same constraints.
                // It's safe to just accumulate the unique IDs here.
                candidates.stream().map(Question::getId).forEach(seenIds::add);
            }
        }
        return shortages;
    }
    
    private List<SelectionShortage> checkStrictAvailability(List<SelectionBucket> buckets, Long subjectId, Long sessionId, Set<Long> excludeIds) {
         // Deep check for cases where deduplication across buckets caused unexpected shortages.
         // Given (subject, session, unit, marks, t) uniqueness, this is mostly for completeness.
         return checkAvailability(buckets, subjectId, sessionId, excludeIds);
    }

    private List<SelectedBucket> performSelection(List<SelectionBucket> buckets, Long subjectId, Long sessionId, Set<Long> excludeIds) {
        List<SelectedBucket> selectedBuckets = new ArrayList<>();
        Set<Long> selectedQuestionIds = new HashSet<>(excludeIds);

        for (SelectionBucket bucket : buckets) {
            List<Question> pool = questionRepo.findBySubjectIdAndSessionIdAndUnitAndMarksAndT(
                    subjectId, sessionId, bucket.getUnit(), bucket.getMarks(), bucket.getT());

            Collections.shuffle(pool, random);

            List<Question> picked = new ArrayList<>();
            for (Question q : pool) {
                if (picked.size() >= bucket.getRequired()) {
                    break;
                }
                if (!selectedQuestionIds.contains(q.getId())) {
                    selectedQuestionIds.add(q.getId());
                    picked.add(q);
                }
            }

            if (picked.size() < bucket.getRequired()) {
                return null; // Signals failure during selection (e.g. due to dedup)
            }
            
            selectedBuckets.add(new SelectedBucket(bucket.getMarks(), bucket.getUnit(), bucket.getT(), picked));
        }

        return selectedBuckets;
    }

    private void validateConsistency(DistributionPlan plan, List<SelectedBucket> selectedBuckets) {
        Set<Long> uniqueIds = new HashSet<>();
        long totalQuestions = 0;

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
                totalQuestions++;
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
