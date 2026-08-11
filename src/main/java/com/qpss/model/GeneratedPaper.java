package com.qpss.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "generated_papers")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GeneratedPaper {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "exam_type", nullable = false)
    private String examType;

    @Column(name = "set_label")
    private String setLabel;

    @Column(name = "generation_date")
    private LocalDateTime generationDate;

    @Column(name = "is_final")
    @Builder.Default
    private Boolean isFinal = false;

    @PrePersist
    void onCreate() {
        this.generationDate = LocalDateTime.now();
    }
}
