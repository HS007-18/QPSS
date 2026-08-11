package com.qpss.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "exam_configs")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamConfig {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_type", nullable = false)
    private String examType;

    @Column(nullable = false)
    private Integer unit;

    @Column(nullable = false)
    private Integer marks;

    @Column(name = "required_count", nullable = false)
    private Integer requiredCount;

    @Column(name = "distribution_pct")
    private BigDecimal distributionPct;
}
