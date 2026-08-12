package com.qpss.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "question_bank_imports")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class QuestionBankImport {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "imported_at", insertable = false, updatable = false)
    private LocalDateTime importedAt;

    @OneToMany(mappedBy = "importBatch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SourceDocument> sourceDocuments;
}
