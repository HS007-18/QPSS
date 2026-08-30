package com.qpss.service;

import com.qpss.service.DistributionCalculationService.SectionConfigInput;
import com.qpss.service.DistributionCalculationService.UnitConfigInput;
import com.qpss.domain.distribution.DistributionPlan;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
public class DistributionCalculationServiceTest {

    private final DistributionCalculationService service = new DistributionCalculationService();

    @Test
    void test40_40_20_Total10() {
        SectionConfigInput input = new SectionConfigInput(2, 10, List.of(
                new UnitConfigInput(1, new BigDecimal("40.00"), new BigDecimal("50.00"), new BigDecimal("50.00")),
                new UnitConfigInput(2, new BigDecimal("40.00"), new BigDecimal("50.00"), new BigDecimal("50.00")),
                new UnitConfigInput(3, new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"))
        ));

        DistributionPlan plan = service.calculate("EXAM", List.of(input));
        List<DistributionPlan.UnitPlan> units = plan.getSections().get(0).getUnits();

        assertEquals(4, getRequired(units, 1));
        assertEquals(4, getRequired(units, 2));
        assertEquals(2, getRequired(units, 3));
    }

    @Test
    void test20_40_40_Total10() {
        SectionConfigInput input = new SectionConfigInput(2, 10, List.of(
                new UnitConfigInput(1, new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00")),
                new UnitConfigInput(2, new BigDecimal("40.00"), new BigDecimal("50.00"), new BigDecimal("50.00")),
                new UnitConfigInput(3, new BigDecimal("40.00"), new BigDecimal("50.00"), new BigDecimal("50.00"))
        ));

        DistributionPlan plan = service.calculate("EXAM", List.of(input));
        List<DistributionPlan.UnitPlan> units = plan.getSections().get(0).getUnits();

        assertEquals(2, getRequired(units, 1));
        assertEquals(4, getRequired(units, 2));
        assertEquals(4, getRequired(units, 3));
    }

    @Test
    void testSemester_20_20_20_20_20_Total10() {
        SectionConfigInput input = new SectionConfigInput(2, 10, List.of(
                new UnitConfigInput(1, new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00")),
                new UnitConfigInput(2, new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00")),
                new UnitConfigInput(3, new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00")),
                new UnitConfigInput(4, new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00")),
                new UnitConfigInput(5, new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"))
        ));

        DistributionPlan plan = service.calculate("EXAM", List.of(input));
        List<DistributionPlan.UnitPlan> units = plan.getSections().get(0).getUnits();

        for (int i = 1; i <= 5; i++) {
            assertEquals(2, getRequired(units, i));
        }
    }

    @Test
    void testLargestRemainder_TieBreaker() {

        SectionConfigInput input = new SectionConfigInput(2, 5, List.of(
                new UnitConfigInput(1, new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00")),
                new UnitConfigInput(2, new BigDecimal("30.00"), new BigDecimal("50.00"), new BigDecimal("50.00")),
                new UnitConfigInput(3, new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("50.00"))
        ));

        DistributionPlan plan = service.calculate("EXAM", List.of(input));
        List<DistributionPlan.UnitPlan> units = plan.getSections().get(0).getUnits();

        assertEquals(1, getRequired(units, 1));
        assertEquals(2, getRequired(units, 2));
        assertEquals(2, getRequired(units, 3));
    }

    @Test
    void testT1_T2_50_50_Even() {
        SectionConfigInput input = new SectionConfigInput(2, 4, List.of(
                new UnitConfigInput(1, new BigDecimal("100.00"), new BigDecimal("50.00"), new BigDecimal("50.00"))
        ));

        DistributionPlan plan = service.calculate("EXAM", List.of(input));
        DistributionPlan.UnitPlan u1 = plan.getSections().get(0).getUnits().get(0);

        assertEquals(4, u1.getRequiredCount());
        assertEquals(2, u1.getT1Required());
        assertEquals(2, u1.getT2Required());
    }

    @Test
    void testT1_T2_50_50_Odd() {

        SectionConfigInput input = new SectionConfigInput(2, 5, List.of(
                new UnitConfigInput(1, new BigDecimal("100.00"), new BigDecimal("50.00"), new BigDecimal("50.00"))
        ));

        DistributionPlan plan = service.calculate("EXAM", List.of(input));
        DistributionPlan.UnitPlan u1 = plan.getSections().get(0).getUnits().get(0);

        assertEquals(5, u1.getRequiredCount());
        assertEquals(3, u1.getT1Required());
        assertEquals(2, u1.getT2Required());
    }

    @Test
    void testT1_100_T2_0() {
        SectionConfigInput input = new SectionConfigInput(2, 5, List.of(
                new UnitConfigInput(1, new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("0.00"))
        ));

        DistributionPlan plan = service.calculate("EXAM", List.of(input));
        DistributionPlan.UnitPlan u1 = plan.getSections().get(0).getUnits().get(0);

        assertEquals(5, u1.getT1Required());
        assertEquals(0, u1.getT2Required());
    }

    @Test
    void testT1_0_T2_100() {
        SectionConfigInput input = new SectionConfigInput(2, 5, List.of(
                new UnitConfigInput(1, new BigDecimal("100.00"), new BigDecimal("0.00"), new BigDecimal("100.00"))
        ));

        DistributionPlan plan = service.calculate("EXAM", List.of(input));
        DistributionPlan.UnitPlan u1 = plan.getSections().get(0).getUnits().get(0);

        assertEquals(0, u1.getT1Required());
        assertEquals(5, u1.getT2Required());
    }

    @Test
    void testZeroOrNegativeTotalRequired() {
        SectionConfigInput zero = new SectionConfigInput(2, 0, List.of(
                new UnitConfigInput(1, new BigDecimal("100.00"), new BigDecimal("50.00"), new BigDecimal("50.00"))
        ));

        assertThrows(IllegalArgumentException.class, () -> {
            service.calculate("EXAM", List.of(zero));
        });

        SectionConfigInput negative = new SectionConfigInput(2, -5, List.of(
                new UnitConfigInput(1, new BigDecimal("100.00"), new BigDecimal("50.00"), new BigDecimal("50.00"))
        ));

        assertThrows(IllegalArgumentException.class, () -> {
            service.calculate("EXAM", List.of(negative));
        });
    }

    @Test
    void testSumAlwaysEqualsTotal() {
        SectionConfigInput input = new SectionConfigInput(2, 17, List.of(
                new UnitConfigInput(1, new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("66.67")),
                new UnitConfigInput(2, new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("66.67")),
                new UnitConfigInput(3, new BigDecimal("33.34"), new BigDecimal("33.34"), new BigDecimal("66.66"))
        ));

        DistributionPlan plan = service.calculate("EXAM", List.of(input));
        List<DistributionPlan.UnitPlan> units = plan.getSections().get(0).getUnits();

        int unitSum = 0;
        for (DistributionPlan.UnitPlan u : units) {
            unitSum += u.getRequiredCount();
            assertEquals(u.getRequiredCount(), u.getT1Required() + u.getT2Required(), "T sums must equal unit required");
        }

        assertEquals(17, unitSum, "Unit sum must equal total required");
    }

    private int getRequired(List<DistributionPlan.UnitPlan> units, int unit) {
        return units.stream().filter(u -> u.getUnit() == unit).findFirst().get().getRequiredCount();
    }
}
