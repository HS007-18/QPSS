package com.qpss.backend.examconfig;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    @Column(name = "t1_pct")
    private BigDecimal t1Pct;

    @Column(name = "t2_pct")
    private BigDecimal t2Pct;

    @Column(name = "t1_required_count")
    private Integer t1RequiredCount;

    @Column(name = "t2_required_count")
    private Integer t2RequiredCount;
}
