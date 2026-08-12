package com.qpss.service;

import com.qpss.model.Question;
import com.qpss.model.QuestionBankImport;
import com.qpss.model.SourceDocument;
import com.qpss.repository.QuestionBankImportRepository;
import com.qpss.repository.QuestionRepository;
import com.qpss.repository.SourceDocumentRepository;
import com.qpss.service.parser.QuestionParseResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionBankImportService {

    private final QuestionBankImportRepository importRepository;
    private final SourceDocumentRepository sourceDocumentRepository;
    private final SourceDocumentStorageService storageService;
    private final QuestionParserService parserService;
    private final QuestionRepository questionRepository;

    public QuestionBankImportService(QuestionBankImportRepository importRepository,
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
    public QuestionBankImportResult createImportBatch(Long subjectId, Long sessionId, List<MultipartFile> files) {
        if (subjectId == null || sessionId == null) {
            throw new IllegalArgumentException("Subject ID and Session ID must be provided.");
        }
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided for import.");
        }

        // --- PHASE 1: Parse ALL files in memory and collect ALL errors across the entire batch ---
        List<FileImportHolder> holders = new ArrayList<>();
        List<String> processedChecksums = new ArrayList<>();
        List<String> allErrors = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                throw new IllegalArgumentException("Empty file: " + file.getOriginalFilename());
            }

            String originalName = file.getOriginalFilename();
            if (originalName == null || !originalName.toLowerCase().endsWith(".docx")) {
                throw new IllegalArgumentException("Unsupported file type or invalid filename: " + originalName);
            }

            String checksum = calculateChecksum(file);

            if (processedChecksums.contains(checksum)) {
                continue; // Skip duplicate within the same batch silently
            }
            processedChecksums.add(checksum);

            // Parse file in memory
            QuestionParseResult parseResult;
            try {
                parseResult = parserService.parseDocx(file);
            } catch (IOException e) {
                allErrors.add(originalName + ": Failed to read DOCX file: " + e.getMessage());
                continue;
            }

            if (parseResult.hasErrors()) {
                for (String error : parseResult.getErrors()) {
                    allErrors.add(originalName + ": " + error);
                }
            }

            holders.add(new FileImportHolder(file, originalName, checksum, parseResult));
        }

        // If ANY file in the batch has a parsing error, abort the entire batch BEFORE any disk/DB mutations
        if (!allErrors.isEmpty()) {
            return QuestionBankImportResult.builder()
                    .successful(false)
                    .parsingErrors(allErrors)
                    .build();
        }

        if (holders.isEmpty()) {
            throw new IllegalArgumentException("No valid documents to import after deduplication.");
        }

        // --- PHASE 2: Persist import batch, source files, and questions atomically ---
        QuestionBankImport importBatch = QuestionBankImport.builder()
                .subjectId(subjectId)
                .sessionId(sessionId)
                .build();

        importBatch = importRepository.save(importBatch);

        List<SourceDocument> savedDocuments = new ArrayList<>();
        List<String> storedFileNames = new ArrayList<>();
        List<Question> allQuestions = new ArrayList<>();

        try {
            for (FileImportHolder holder : holders) {
                // Check if duplicate against existing batches in DB
                boolean existsInDb = sourceDocumentRepository.existsByImportBatchIdAndChecksum(importBatch.getId(), holder.checksum);
                if (existsInDb) {
                    continue;
                }

                String extension = ".docx";
                String storedFileName = storageService.storeDocument(holder.file, extension);
                storedFileNames.add(storedFileName);

                SourceDocument doc = SourceDocument.builder()
                        .importBatch(importBatch)
                        .originalFileName(holder.originalName)
                        .storedFileName(storedFileName)
                        .fileExtension(extension)
                        .contentType(holder.file.getContentType())
                        .fileSize(holder.file.getSize())
                        .checksum(holder.checksum)
                        .build();

                doc = sourceDocumentRepository.save(doc);
                savedDocuments.add(doc);

                List<Question> questions = parserService.toQuestions(
                        holder.parseResult.getValidQuestions(),
                        subjectId,
                        sessionId,
                        doc.getId(),
                        holder.originalName
                );
                allQuestions.addAll(questions);
            }

            importBatch.setSourceDocuments(savedDocuments);
            questionRepository.saveAll(allQuestions);

            return QuestionBankImportResult.builder()
                    .importBatch(importBatch)
                    .questionsParsed(allQuestions.size())
                    .parsingErrors(allErrors)
                    .successful(true)
                    .build();

        } catch (Exception e) {
            // Clean up any files saved to disk in Phase 2 if DB commit fails
            for (String storedFileName : storedFileNames) {
                storageService.deleteDocument(storedFileName);
            }
            throw new RuntimeException("Failed to process import batch. Rolled back stored files.", e);
        }
    }

    private static class FileImportHolder {
        final MultipartFile file;
        final String originalName;
        final String checksum;
        final QuestionParseResult parseResult;

        FileImportHolder(MultipartFile file, String originalName, String checksum, QuestionParseResult parseResult) {
            this.file = file;
            this.originalName = originalName;
            this.checksum = checksum;
            this.parseResult = parseResult;
        }
    }

    private String calculateChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) > 0) {
                    digest.update(buffer, 0, read);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException("Failed to calculate checksum", e);
        }
    }
}
