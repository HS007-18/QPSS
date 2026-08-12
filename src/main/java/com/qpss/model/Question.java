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

    @Column(name = "raw_ooxml", columnDefinition = "LONGTEXT")
    private String rawOoxml;

    @Column(name = "source_file_name")
    private String sourceFileName;

    @Column(name = "source_page_number")
    private Integer sourcePageNumber;

    @Column(nullable = false)
    private Integer t;

    @PrePersist
    @PreUpdate
    private void validateT() {
        if (t == null || (t != 1 && t != 2)) {
            throw new IllegalStateException("Question T value must be exactly 1 or 2. Found: " + t);
        }
    }
}
