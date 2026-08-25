package com.qpss.common.domain;
import lombok.Builder;
import lombok.Value;
import java.math.BigDecimal;
import java.util.List;
@Value
@Builder
public class DistributionPlan {
    String examType;
    List<SectionPlan> sections;

    @Value
    @Builder
    public static class SectionPlan {
        int marks;
        int totalRequired;
        List<UnitPlan> units;
    }

    @Value
    @Builder
    public static class UnitPlan {
        int unit;
        BigDecimal percentage;
        int requiredCount;
        BigDecimal t1Percentage;
        int t1Required;
        BigDecimal t2Percentage;
        int t2Required;
    }
}