package com.qpss.backend.questionbank;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
