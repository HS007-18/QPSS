package com.qpss.backend.selection;
import com.qpss.common.domain.DistributionPlan;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
@Service
public class DistributionCalculationService {

    public static class UnitConfigInput {
        private final int unit;
        private final BigDecimal percentage;
        private final BigDecimal t1Percentage;
        private final BigDecimal t2Percentage;

        public UnitConfigInput(int unit, BigDecimal percentage, BigDecimal t1Percentage, BigDecimal t2Percentage) {
            this.unit = unit;
            this.percentage = percentage;
            this.t1Percentage = t1Percentage;
            this.t2Percentage = t2Percentage;
        }

        public int getUnit() {
            return unit;
        }

        public BigDecimal getPercentage() {
            return percentage;
        }

        public BigDecimal getT1Percentage() {
            return t1Percentage;
        }

        public BigDecimal getT2Percentage() {
            return t2Percentage;
        }
    }

    public static class SectionConfigInput {
        private final int marks;
        private final int totalRequired;
        private final List<UnitConfigInput> units;

        public SectionConfigInput(int marks, int totalRequired, List<UnitConfigInput> units) {
            this.marks = marks;
            this.totalRequired = totalRequired;
            this.units = units;
        }

        public int getMarks() {
            return marks;
        }

        public int getTotalRequired() {
            return totalRequired;
        }

        public List<UnitConfigInput> getUnits() {
            return units;
        }
    }

    private static class AllocationItem {
        Object key;
        int baseCount;
        double remainder;

        AllocationItem(Object key, int baseCount, double remainder) {
            this.key = key;
            this.baseCount = baseCount;
            this.remainder = remainder;
        }
    }

    public DistributionPlan calculate(String examType, List<SectionConfigInput> sections) {
        List<DistributionPlan.SectionPlan> sectionPlans = new ArrayList<>();

        for (SectionConfigInput section : sections) {
            List<DistributionPlan.UnitPlan> unitPlans = calculateSection(section);
            sectionPlans.add(DistributionPlan.SectionPlan.builder()
                    .marks(section.getMarks())
                    .totalRequired(section.getTotalRequired())
                    .units(unitPlans)
                    .build());
        }

        return DistributionPlan.builder()
                .examType(examType)
                .sections(sectionPlans)
                .build();
    }

    private List<DistributionPlan.UnitPlan> calculateSection(SectionConfigInput section) {
        int totalRequired = section.getTotalRequired();
        List<UnitConfigInput> units = section.getUnits();

        if (totalRequired <= 0) {
            throw new IllegalArgumentException("Total required must be greater than 0");
        }

        List<AllocationItem> allocations = new ArrayList<>();
        int currentSum = 0;

        for (UnitConfigInput u : units) {
            double raw = totalRequired * u.getPercentage().doubleValue() / 100.0;
            int base = (int) Math.floor(raw);
            double remainder = raw - base;
            allocations.add(new AllocationItem(u, base, remainder));
            currentSum += base;
        }

        int remaining = Math.min(totalRequired - currentSum, allocations.size());

        allocations.sort((a, b) -> {
            int cmp = Double.compare(b.remainder, a.remainder);
            if (cmp != 0) return cmp;
            return Integer.compare(((UnitConfigInput)a.key).getUnit(), ((UnitConfigInput)b.key).getUnit());
        });

        for (int i = 0; i < remaining; i++) {
            allocations.get(i).baseCount++;
        }

        allocations.sort(Comparator.comparingInt(a -> ((UnitConfigInput)a.key).getUnit()));

        List<DistributionPlan.UnitPlan> unitPlans = new ArrayList<>();
        for (AllocationItem item : allocations) {
            UnitConfigInput u = (UnitConfigInput) item.key;
            int unitRequired = item.baseCount;

            int[] tAllocations = calculateTAllocation(unitRequired, u.getT1Percentage().doubleValue(), u.getT2Percentage().doubleValue());

            unitPlans.add(DistributionPlan.UnitPlan.builder()
                    .unit(u.getUnit())
                    .percentage(u.getPercentage())
                    .requiredCount(unitRequired)
                    .t1Percentage(u.getT1Percentage())
                    .t1Required(tAllocations[0])
                    .t2Percentage(u.getT2Percentage())
                    .t2Required(tAllocations[1])
                    .build());
        }

        return unitPlans;
    }

    private int[] calculateTAllocation(int unitRequired, double t1Pct, double t2Pct) {
        if (unitRequired == 0) return new int[]{0, 0};

        double rawT1 = unitRequired * t1Pct / 100.0;
        double rawT2 = unitRequired * t2Pct / 100.0;

        int baseT1 = (int) Math.floor(rawT1);
        int baseT2 = (int) Math.floor(rawT2);

        double remT1 = rawT1 - baseT1;
        double remT2 = rawT2 - baseT2;

        int remaining = Math.min(unitRequired - (baseT1 + baseT2), 2);

        List<AllocationItem> items = Arrays.asList(
                new AllocationItem(1, baseT1, remT1),
                new AllocationItem(2, baseT2, remT2)
        );

        items.sort((a, b) -> {
            int cmp = Double.compare(b.remainder, a.remainder);
            if (cmp != 0) return cmp;
            return Integer.compare((Integer)a.key, (Integer)b.key);
        });

        for (int i = 0; i < remaining; i++) {
            items.get(i).baseCount++;
        }

        items.sort(Comparator.comparingInt(a -> (Integer)a.key));

        return new int[]{items.get(0).baseCount, items.get(1).baseCount};
    }
}
