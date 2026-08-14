package com.qpss.service;

import com.qpss.repository.QuestionRepository;
import com.qpss.service.distribution.DistributionPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DiversityAnalyzer {

    private final QuestionRepository questionRepo;

    public String check(DistributionPlan plan, Long sessionId, int numSets) {
        if (numSets <= 1) return null;

        int minUniqueSets = Integer.MAX_VALUE;

        for (DistributionPlan.SectionPlan section : plan.getSections()) {
            for (DistributionPlan.UnitPlan rule : section.getUnits()) {
                if (rule.getRequiredCount() == 0) continue;
                int poolSize = questionRepo.countBySessionIdAndUnitAndMarks(
                        sessionId, rule.getUnit(), section.getMarks());
                int maxSets = poolSize / rule.getRequiredCount();
                minUniqueSets = Math.min(minUniqueSets, maxSets);
            }
        }

        if (numSets > minUniqueSets) {
            return "Only " + minUniqueSets + " meaningfully different sets possible. "
                    + "Requested " + numSets + " — some questions will repeat across sets.";
        }
        return null;
    }
}
