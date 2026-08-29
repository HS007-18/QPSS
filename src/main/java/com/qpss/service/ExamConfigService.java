package com.qpss.service;

import com.qpss.repository.ExamConfigRepository;
import com.qpss.entity.ExamConfig;
import com.qpss.repository.ExamSectionConfigRepository;
import com.qpss.entity.ExamSectionConfig;
import com.qpss.service.DistributionCalculationService;
import com.qpss.domain.distribution.DistributionPlan;
import com.qpss.document.model.QuestionConstants;
import com.qpss.domain.ExamType;
import com.qpss.domain.ExamFormat;
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

    @Transactional(readOnly = true)
    public DistributionPlan getDistributionPlan(ExamType examType, ExamFormat format) {
        if (examType == null) {
            throw new IllegalArgumentException("Invalid exam type");
        }

        List<ExamSectionConfig> sections = sectionConfigRepo.findAll().stream()
                .filter(s -> s.getExamType().equals(examType.name()))
                .filter(s -> {
                    if (format == ExamFormat.FORMAT_2) {
                        return s.getMarks() == 20;
                    } else {

                        return s.getMarks() == 2 || s.getMarks() == 16;
                    }
                })
                .sorted(Comparator.comparingInt(ExamSectionConfig::getMarks))
                .collect(Collectors.toList());

        if (sections.isEmpty()) {
            throw new IllegalArgumentException("No configuration found for exam type: " + examType.name());
        }

        List<ExamConfig> unitConfigs = configRepo.findByExamTypeOrderByMarksAscUnitAsc(examType.name());

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

        return calculationService.calculate(examType.name(), sectionInputs);
    }

    @Transactional
    public DistributionPlan saveConfiguration(ExamType examType, List<DistributionCalculationService.SectionConfigInput> sectionInputs) {
        if (examType == null) {
            throw new IllegalArgumentException("Invalid exam type");
        }

        for (DistributionCalculationService.SectionConfigInput section : sectionInputs) {
            if (!QuestionConstants.MARK_VALUES.contains(section.getMarks())) {
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

        DistributionPlan plan = calculationService.calculate(examType.name(), sectionInputs);

        List<ExamSectionConfig> existingSections = sectionConfigRepo.findAll().stream()
                .filter(s -> s.getExamType().equals(examType.name()))
                .collect(Collectors.toList());
        sectionConfigRepo.deleteAll(existingSections);

        List<ExamConfig> existingUnits = configRepo.findByExamTypeOrderByMarksAscUnitAsc(examType.name());
        configRepo.deleteAll(existingUnits);

        for (DistributionPlan.SectionPlan sp : plan.getSections()) {
            sectionConfigRepo.save(ExamSectionConfig.builder()
                    .examType(examType.name())
                    .marks(sp.getMarks())
                    .totalRequired(sp.getTotalRequired())
                    .build());

            for (DistributionPlan.UnitPlan up : sp.getUnits()) {
                configRepo.save(ExamConfig.builder()
                        .examType(examType.name())
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
