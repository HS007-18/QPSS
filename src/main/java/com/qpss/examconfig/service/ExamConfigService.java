package com.qpss.examconfig.service;

import com.qpss.examconfig.model.ExamConfig;
import com.qpss.examconfig.model.ExamSectionConfig;
import com.qpss.examconfig.repository.ExamConfigRepository;
import com.qpss.examconfig.repository.ExamSectionConfigRepository;
import com.qpss.generation.distribution.DistributionCalculationService;
import com.qpss.generation.distribution.DistributionPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExamConfigService {

    private final ExamConfigRepository configRepo;
    private final ExamSectionConfigRepository sectionConfigRepo;
    private final DistributionCalculationService calculationService;

    private static final Set<Integer> SUPPORTED_MARKS = Set.of(2, 16, 20);

    @Transactional(readOnly = true)
    public DistributionPlan getDistributionPlan(String examType, String format) {
        if (examType == null || examType.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid exam type");
        }

        List<ExamSectionConfig> sections = sectionConfigRepo.findAll().stream()
                .filter(s -> s.getExamType().equals(examType))
                .filter(s -> {
                    if ("FORMAT_2".equals(format)) {
                        return s.getMarks() == 20;
                    } else {

                        return s.getMarks() == 2 || s.getMarks() == 16;
                    }
                })
                .sorted(Comparator.comparingInt(ExamSectionConfig::getMarks))
                .collect(Collectors.toList());

        if (sections.isEmpty()) {
            throw new IllegalArgumentException("No configuration found for exam type: " + examType);
        }

        List<ExamConfig> unitConfigs = configRepo.findByExamTypeOrderByMarksAscUnitAsc(examType);

        List<DistributionCalculationService.SectionConfigInput> sectionInputs = new ArrayList<>();

        for (ExamSectionConfig section : sections) {
            int required = section.getTotalRequired();

            List<DistributionCalculationService.UnitConfigInput> unitInputs = unitConfigs.stream()
                    .filter(c -> c.getMarks().equals(section.getMarks()))
                    .map(c -> new DistributionCalculationService.UnitConfigInput(
                            c.getUnit(), c.getDistributionPct(), c.getT1Pct(), c.getT2Pct()
                    ))
                    .collect(Collectors.toList());

            sectionInputs.add(new DistributionCalculationService.SectionConfigInput(
                    section.getMarks(), required, unitInputs
            ));
        }

        return calculationService.calculate(examType, sectionInputs);
    }

    @Transactional(readOnly = true)
    public DistributionPlan getDistributionPlan(String examType) {
        return getDistributionPlan(examType, "FORMAT_1");
    }

    @Transactional
    public DistributionPlan saveConfiguration(String examType, List<DistributionCalculationService.SectionConfigInput> sectionInputs) {
        if (examType == null || examType.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid exam type");
        }

        for (DistributionCalculationService.SectionConfigInput section : sectionInputs) {
            if (!SUPPORTED_MARKS.contains(section.getMarks())) {
                throw new IllegalArgumentException("Unsupported marks: " + section.getMarks());
            }
            if (section.getTotalRequired() <= 0) {
                throw new IllegalArgumentException("Total required must be > 0");
            }

            BigDecimal unitSum = BigDecimal.ZERO;
            Set<Integer> seenUnits = new HashSet<>();

            for (DistributionCalculationService.UnitConfigInput u : section.getUnits()) {
                if (!seenUnits.add(u.getUnit())) {
                    throw new IllegalArgumentException("Duplicate unit configuration for unit " + u.getUnit());
                }

                validatePercentage(u.getPercentage(), "Unit percentage");
                validatePercentage(u.getT1Percentage(), "T1 percentage");
                validatePercentage(u.getT2Percentage(), "T2 percentage");

                if (u.getT1Percentage().add(u.getT2Percentage()).compareTo(new BigDecimal("100.00")) != 0 &&
                    u.getT1Percentage().add(u.getT2Percentage()).compareTo(new BigDecimal("100.0")) != 0 &&
                    u.getT1Percentage().add(u.getT2Percentage()).compareTo(new BigDecimal("100")) != 0) {
                    throw new IllegalArgumentException("T1 + T2 percentage must exactly equal 100 for unit " + u.getUnit());
                }

                unitSum = unitSum.add(u.getPercentage());
            }

            if (unitSum.compareTo(new BigDecimal("100.00")) != 0 &&
                unitSum.compareTo(new BigDecimal("100.0")) != 0 &&
                unitSum.compareTo(new BigDecimal("100")) != 0) {
                throw new IllegalArgumentException("Sum of unit percentages must exactly equal 100 for marks " + section.getMarks());
            }
        }

        DistributionPlan plan = calculationService.calculate(examType, sectionInputs);

        List<ExamSectionConfig> existingSections = sectionConfigRepo.findAll().stream()
                .filter(s -> s.getExamType().equals(examType))
                .collect(Collectors.toList());
        sectionConfigRepo.deleteAll(existingSections);

        List<ExamConfig> existingUnits = configRepo.findByExamTypeOrderByMarksAscUnitAsc(examType);
        configRepo.deleteAll(existingUnits);

        for (DistributionPlan.SectionPlan sp : plan.getSections()) {
            sectionConfigRepo.save(ExamSectionConfig.builder()
                    .examType(examType)
                    .marks(sp.getMarks())
                    .totalRequired(sp.getTotalRequired())
                    .build());

            for (DistributionPlan.UnitPlan up : sp.getUnits()) {
                configRepo.save(ExamConfig.builder()
                        .examType(examType)
                        .marks(sp.getMarks())
                        .unit(up.getUnit())
                        .distributionPct(up.getPercentage())
                        .requiredCount(up.getRequiredCount())
                        .t1Pct(up.getT1Percentage())
                        .t1RequiredCount(up.getT1Required())
                        .t2Pct(up.getT2Percentage())
                        .t2RequiredCount(up.getT2Required())
                        .build());
            }
        }

        return plan;
    }

    private void validatePercentage(BigDecimal pct, String name) {
        if (pct == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
        if (pct.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
        if (pct.compareTo(new BigDecimal("100.00")) > 0) {
            throw new IllegalArgumentException(name + " cannot be greater than 100");
        }
    }
}
