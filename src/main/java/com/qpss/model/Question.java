package com.qpss.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "questions")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Question {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "source_document_id")
    private Long sourceDocumentId;

    @Column(nullable = false)
    private Integer unit;

    @Column(nullable = false)
    private String co;

    @Column(nullable = false)
    private Integer marks;

    @Column(name = "serial_no")
    private Integer serialNo;

    @Column(name = "question_content", nullable = false, columnDefinition = "TEXT")
    private String questionContent;

    @Column(name = "source_file_name")
    private String sourceFileName;

    @Column(nullable = false)
    private Integer t;

    @Column(nullable = false, length = 10)
    private String rbt;

    @PrePersist
    @PreUpdate
    private void validateT() {
        if (t == null || (t != 1 && t != 2)) {
            throw new IllegalStateException("Question T value must be exactly 1 or 2. Found: " + t);
        }
    }
}
