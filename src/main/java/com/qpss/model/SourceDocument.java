package com.qpss.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "source_documents")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SourceDocument {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "import_batch_id", nullable = false)
    @ToString.Exclude
    private QuestionBankImport importBatch;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "stored_file_name", nullable = false)
    private String storedFileName;

    @Column(name = "file_extension", nullable = false, length = 10)
    private String fileExtension;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 64)
    private String checksum;

    @Column(name = "uploaded_at", insertable = false, updatable = false)
    private LocalDateTime uploadedAt;
}
