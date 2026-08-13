package com.qpss.service;

import com.qpss.dto.PendingUploadSession;
import com.qpss.dto.QuestionBankImportResult;
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

    public PendingUploadSession prepareImportBatch(Long subjectId, Long sessionId, List<MultipartFile> files) {
        if (subjectId == null || sessionId == null) {
            throw new IllegalArgumentException("Subject ID and Session ID must be provided.");
        }
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided for import.");
        }

        PendingUploadSession session = new PendingUploadSession();
        session.setSubjectId(subjectId);
        session.setSessionId(sessionId);
        List<PendingUploadSession.FileImportHolder> holders = new ArrayList<>();
        List<String> processedChecksums = new ArrayList<>();

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
                continue;
            }
            processedChecksums.add(checksum);

            QuestionParseResult parseResult;
            try {
                parseResult = parserService.parseDocx(file);
            } catch (IOException e) {
                throw new RuntimeException("Failed to read DOCX file: " + e.getMessage());
            }

            PendingUploadSession.FileImportHolder holder = new PendingUploadSession.FileImportHolder();
            holder.setOriginalName(originalName);
            holder.setChecksum(checksum);
            holder.setParseResult(parseResult);
            holder.setContentType(file.getContentType());
            holder.setSize(file.getSize());
            try {
                holder.setFileBytes(file.getBytes());
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file content.", e);
            }
            holders.add(holder);
        }

        if (holders.isEmpty()) {
            throw new IllegalArgumentException("No valid documents to import after deduplication.");
        }
        session.setFiles(holders);
        return session;
    }

    @Transactional
    public QuestionBankImportResult commitImportBatch(PendingUploadSession pendingSession) {
        QuestionBankImport importBatch = QuestionBankImport.builder()
                .subjectId(pendingSession.getSubjectId())
                .sessionId(pendingSession.getSessionId())
                .build();

        importBatch = importRepository.save(importBatch);

        List<SourceDocument> savedDocuments = new ArrayList<>();
        List<String> storedFileNames = new ArrayList<>();
        List<Question> allQuestions = new ArrayList<>();
        List<String> allErrors = new ArrayList<>();

        try {
            for (PendingUploadSession.FileImportHolder holder : pendingSession.getFiles()) {
                if (sourceDocumentRepository.existsByChecksum(holder.getChecksum())) {
                    continue;
                }

                if (holder.getParseResult().hasErrors()) {
                    for (String error : holder.getParseResult().getErrors()) {
                        allErrors.add(holder.getOriginalName() + ": " + error);
                    }
                    continue;
                }

                String extension = ".docx";
                String storedFileName = storageService.storeDocument(holder.getFileBytes(), extension);
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
                allQuestions.addAll(questions);
            }

            if (allQuestions.isEmpty() && !allErrors.isEmpty()) {
                return QuestionBankImportResult.builder()
                        .successful(false)
                        .parsingErrors(allErrors)
                        .build();
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
            for (String storedFileName : storedFileNames) {
                storageService.deleteDocument(storedFileName);
            }
            throw new RuntimeException("Failed to process import batch. Rolled back stored files.", e);
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