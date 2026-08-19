package com.qpss.backend.questionbank;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    @Column(name = "marks_split", length = 20)
    private String marksSplit;

    @Column(name = "question_type", length = 10)
    private String questionType;

    @PrePersist
    @PreUpdate
    private void validateT() {
        if (t == null || (t != 1 && t != 2)) {
            throw new IllegalStateException("Question T value must be exactly 1 or 2. Found: " + t);
        }
    }
}
