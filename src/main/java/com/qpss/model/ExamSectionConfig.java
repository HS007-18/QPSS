package com.qpss.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exam_section_configs")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamSectionConfig {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_type", nullable = false)
    private String examType;

    @Column(nullable = false)
    private Integer marks;

    @Column(name = "total_required", nullable = false)
    private Integer totalRequired;
}
