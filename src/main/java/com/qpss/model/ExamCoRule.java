package com.qpss.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exam_co_rules")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ExamCoRule {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "exam_type", nullable = false)
    private String examType;

    @Column(nullable = false)
    private String co;
}
