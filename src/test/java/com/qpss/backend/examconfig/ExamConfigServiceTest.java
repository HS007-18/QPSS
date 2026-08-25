package com.qpss.backend.examconfig;
import com.qpss.backend.selection.DistributionCalculationService;
import com.qpss.common.domain.DistributionPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
public class ExamConfigServiceTest {

    @Mock
    private ExamConfigRepository configRepo;

    @Mock
    private ExamSectionConfigRepository sectionConfigRepo;

    @Mock
    private DistributionCalculationService calculationService;

    @InjectMocks
    private ExamConfigService examConfigService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testValidConfigurationSave() {
        DistributionCalculationService.SectionConfigInput input = new DistributionCalculationService.SectionConfigInput(2, 10, List.of(
                new DistributionCalculationService.UnitConfigInput(1, new BigDecimal("40.00"), new BigDecimal("50.00"), new BigDecimal("50.00")),
                new DistributionCalculationService.UnitConfigInput(2, new BigDecimal("40.00"), new BigDecimal("50.00"), new BigDecimal("50.00")),
                new DistributionCalculationService.UnitConfigInput(3, new BigDecimal("20.00"), new BigDecimal("50.00"), new BigDecimal("50.00"))
        ));

        DistributionPlan mockPlan = DistributionPlan.builder()
                .examType("INTERNAL_1")
                .sections(List.of(DistributionPlan.SectionPlan.builder()
                        .marks(2)
                        .totalRequired(10)
                        .units(List.of(
                                DistributionPlan.UnitPlan.builder().unit(1).percentage(new BigDecimal("40.00")).requiredCount(4).t1Required(2).t2Required(2).build(),
                                DistributionPlan.UnitPlan.builder().unit(2).percentage(new BigDecimal("40.00")).requiredCount(4).t1Required(2).t2Required(2).build(),
                                DistributionPlan.UnitPlan.builder().unit(3).percentage(new BigDecimal("20.00")).requiredCount(2).t1Required(1).t2Required(1).build()
                        ))
                        .build()))
                .build();

        when(calculationService.calculate(eq("INTERNAL_1"), any())).thenReturn(mockPlan);

        DistributionPlan result = examConfigService.saveConfiguration("INTERNAL_1", List.of(input));

        assertNotNull(result);
        verify(sectionConfigRepo).deleteAll(any());
        verify(configRepo).deleteAll(any());

    }

    @Test
    void testInvalidPercentageTotals() {
        DistributionCalculationService.SectionConfigInput input = new DistributionCalculationService.SectionConfigInput(2, 10, List.of(
                new DistributionCalculationService.UnitConfigInput(1, new BigDecimal("40.00"), new BigDecimal("50.00"), new BigDecimal("50.00")),
                new DistributionCalculationService.UnitConfigInput(2, new BigDecimal("40.00"), new BigDecimal("50.00"), new BigDecimal("50.00"))

        ));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            examConfigService.saveConfiguration("INTERNAL_1", List.of(input));
        });
        assertTrue(ex.getMessage().contains("exactly equal 100"));
    }

    @Test
    void testInvalidTDistribution() {
        DistributionCalculationService.SectionConfigInput input = new DistributionCalculationService.SectionConfigInput(2, 10, List.of(
                new DistributionCalculationService.UnitConfigInput(1, new BigDecimal("100.00"), new BigDecimal("60.00"), new BigDecimal("30.00"))
        ));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            examConfigService.saveConfiguration("INTERNAL_1", List.of(input));
        });
        assertTrue(ex.getMessage().contains("T1 + T2 percentage must exactly equal 100"));
    }

    @Test
    void testNegativePercentages() {
        DistributionCalculationService.SectionConfigInput input = new DistributionCalculationService.SectionConfigInput(2, 10, List.of(
                new DistributionCalculationService.UnitConfigInput(1, new BigDecimal("100.00"), new BigDecimal("-10.00"), new BigDecimal("110.00"))
        ));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            examConfigService.saveConfiguration("INTERNAL_1", List.of(input));
        });
        assertTrue(ex.getMessage().contains("cannot be negative"));
    }

    @Test
    void testInvalidMarks() {
        DistributionCalculationService.SectionConfigInput input = new DistributionCalculationService.SectionConfigInput(3, 10, List.of(
                new DistributionCalculationService.UnitConfigInput(1, new BigDecimal("100.00"), new BigDecimal("50.00"), new BigDecimal("50.00"))
        ));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            examConfigService.saveConfiguration("INTERNAL_1", List.of(input));
        });
        assertTrue(ex.getMessage().contains("Unsupported marks"));
    }

    @Test
    void testDuplicateUnit() {
        DistributionCalculationService.SectionConfigInput input = new DistributionCalculationService.SectionConfigInput(2, 10, List.of(
                new DistributionCalculationService.UnitConfigInput(1, new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("50.00")),
                new DistributionCalculationService.UnitConfigInput(1, new BigDecimal("50.00"), new BigDecimal("50.00"), new BigDecimal("50.00"))
        ));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            examConfigService.saveConfiguration("INTERNAL_1", List.of(input));
        });
        assertTrue(ex.getMessage().contains("Duplicate unit configuration"));
    }
}
