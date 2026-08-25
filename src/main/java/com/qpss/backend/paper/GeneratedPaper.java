package com.qpss.backend.paper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    @Column(name = "exam_session")
    private String examSession;

    @Column(name = "exam_title")
    private String examTitle;

    @Column(name = "duration")
    private String duration;

    @PrePersist
    void onCreate() {
        this.generationDate = LocalDateTime.now();
    }
}
