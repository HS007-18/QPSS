package com.qpss.backend.questionbank;
import com.qpss.backend.questionbank.dto.PendingUploadSession;
import com.qpss.backend.questionbank.dto.QuestionBankImportResult;
import com.qpss.documentextraction.model.QuestionParseResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
        if (files.size() > 20) {
            throw new IllegalArgumentException("Maximum 20 files allowed per upload.");
        }
        long totalSize = files.stream().mapToLong(MultipartFile::getSize).sum();
        if (totalSize > 50 * 1024 * 1024) {
            throw new IllegalArgumentException("Total upload size exceeds 50MB limit.");
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

            Path tempFile;
            try {
                tempFile = Files.createTempFile("qpss_upload_", ".docx");
                file.transferTo(tempFile.toFile());
            } catch (IOException e) {
                throw new RuntimeException("Failed to write temporary file.", e);
            }

            PendingUploadSession.FileImportHolder holder = new PendingUploadSession.FileImportHolder();
            holder.setIndex(holders.size());
            holder.setOriginalName(originalName);
            holder.setChecksum(checksum);
            holder.setParseResult(parseResult);
            holder.setTempFilePath(tempFile.toString());
            holder.setContentType(file.getContentType());
            holder.setSize(file.getSize());
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

    private String normalizeForDuplicateCheck(String content) {
        if (content == null) return "";
        String stripped = content.replaceAll("<[^>]*>", " ");
        stripped = stripped.replaceAll("[^a-zA-Z0-9]", "");
        return stripped.toLowerCase();
    }
}