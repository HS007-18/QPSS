package com.qpss.service;

import com.qpss.repository.SourceDocumentRepository;
import com.qpss.repository.QuestionBankImportRepository;
import com.qpss.entity.Question;
import com.qpss.repository.QuestionRepository;
import com.qpss.entity.SourceDocument;
import com.qpss.entity.QuestionBankImport;
import com.qpss.dto.PendingUploadSession;
import com.qpss.dto.QuestionBankImportResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImportBatchCommitter {

    private final QuestionBankImportRepository importRepository;
    private final SourceDocumentRepository sourceDocumentRepository;
    private final SourceDocumentStorageService storageService;
    private final QuestionParserService parserService;
    private final QuestionRepository questionRepository;

    public ImportBatchCommitter(QuestionBankImportRepository importRepository,
                                SourceDocumentRepository sourceDocumentRepository,
                                SourceDocumentStorageService storageService,
                                QuestionParserService parserService,
                                QuestionRepository questionRepository) {
        this.importRepository = importRepository;
        this.sourceDocumentRepository = sourceDocumentRepository;
        this.storageService = storageService;
        this.parserService = parserService;
        this.questionRepository = questionRepository;
    }

    @Transactional
    public QuestionBankImportResult commitImportBatch(PendingUploadSession pendingSession) {
        List<String> allErrors = new ArrayList<>();
        int skippedDuplicates = 0;
        boolean anyImportable = false;

        for (PendingUploadSession.FileImportHolder holder : pendingSession.getFiles()) {
            if (holder.getParseResult().getValidQuestions().isEmpty()) {
                for (String error : holder.getParseResult().getErrors()) {
                    allErrors.add(holder.getOriginalName() + ": " + error);
                }
            } else if (sourceDocumentRepository.existsByChecksumAndImportBatch_SessionId(
                    holder.getChecksum(), pendingSession.getSessionId())) {
                skippedDuplicates++;
            } else {
                anyImportable = true;
                for (String error : holder.getParseResult().getErrors()) {
                    allErrors.add(holder.getOriginalName() + ": " + error);
                }
            }
        }

        if (!anyImportable) {
            cleanupTempFiles(pendingSession);
            if (!allErrors.isEmpty()) {
                return QuestionBankImportResult.builder()
                        .successful(false)
                        .parsingErrors(allErrors)
                        .build();
            }
            return QuestionBankImportResult.builder()
                    .successful(false)
                    .parsingErrors(List.of("All uploaded file(s) are duplicates of questions already imported in this session."))
                    .build();
        }

        QuestionBankImport importBatch = QuestionBankImport.builder()
                .subjectId(pendingSession.getSubjectId())
                .sessionId(pendingSession.getSessionId())
                .build();

        importBatch = importRepository.save(importBatch);

        List<SourceDocument> savedDocuments = new ArrayList<>();
        List<String> storedFileNames = new ArrayList<>();
        List<Question> allQuestions = new ArrayList<>();

        List<Question> existingQuestions = questionRepository.findBySubjectIdOrderByUnitAscSerialNoAsc(pendingSession.getSubjectId());
        java.util.Set<String> seenContents = new java.util.HashSet<>();
        for (Question eq : existingQuestions) {
            seenContents.add(normalizeForDuplicateCheck(eq.getQuestionContent()));
        }

        try {
            for (PendingUploadSession.FileImportHolder holder : pendingSession.getFiles()) {
                if (holder.getParseResult().getValidQuestions().isEmpty()) {
                    continue;
                }
                if (sourceDocumentRepository.existsByChecksumAndImportBatch_SessionId(
                        holder.getChecksum(), pendingSession.getSessionId())) {
                    continue;
                }

                String extension = ".docx";
                String storedFileName = storageService.storeDocument(holder.getTempFilePath(), extension);
                storedFileNames.add(storedFileName);

                SourceDocument doc = SourceDocument.builder()
                        .importBatch(importBatch)
                        .originalFileName(holder.getOriginalName())
                        .storedFileName(storedFileName)
                        .fileExtension(extension)
                        .contentType(holder.getContentType())
                        .fileSize(holder.getSize())
                        .checksum(holder.getChecksum())
                        .build();

                doc = sourceDocumentRepository.save(doc);
                savedDocuments.add(doc);

                List<Question> questions = parserService.toQuestions(
                        holder.getParseResult().getValidQuestions(),
                        pendingSession.getSubjectId(),
                        pendingSession.getSessionId(),
                        doc.getId(),
                        holder.getOriginalName()
                );
                
                for (Question q : questions) {
                    String norm = normalizeForDuplicateCheck(q.getQuestionContent());
                    if (seenContents.add(norm)) {
                        allQuestions.add(q);
                    } else {
                        skippedDuplicates++;
                    }
                }
            }

            if (!allQuestions.isEmpty()) {
                importBatch.setSourceDocuments(savedDocuments);
                questionRepository.saveAll(allQuestions);
            }

            cleanupTempFiles(pendingSession);

            return QuestionBankImportResult.builder()
                    .importBatch(importBatch)
                    .questionsParsed(allQuestions.size())
                    .skippedDuplicates(skippedDuplicates)
                    .parsingErrors(allErrors)
                    .successful(true)
                    .build();

        } catch (Exception e) {
            for (String storedFileName : storedFileNames) {
                storageService.deleteDocument(storedFileName);
            }
            cleanupTempFiles(pendingSession);
            throw new RuntimeException("Failed to process import batch. Rolled back stored files.", e);
        }
    }

    private void cleanupTempFiles(PendingUploadSession pendingSession) {
        if (pendingSession.getFiles() != null) {
            for (PendingUploadSession.FileImportHolder holder : pendingSession.getFiles()) {
                if (holder.getTempFilePath() != null) {
                    try {
                        Files.deleteIfExists(Path.of(holder.getTempFilePath()));
                    } catch (IOException ignored) {
                    }
                }
            }
        }
    }

    private String normalizeForDuplicateCheck(String content) {
        if (content == null) return "";
        String stripped = content.replaceAll("<[^>]*>", " ");
        stripped = stripped.replaceAll("[^a-zA-Z0-9]", "");
        return stripped.toLowerCase();
    }
}
