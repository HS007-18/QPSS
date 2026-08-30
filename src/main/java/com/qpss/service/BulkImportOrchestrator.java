package com.qpss.service;

import com.qpss.entity.QuestionBankImport;
import com.qpss.repository.QuestionBankImportRepository;
import com.qpss.entity.Question;
import com.qpss.entity.SourceDocument;
import com.qpss.repository.SourceDocumentRepository;
import com.qpss.repository.QuestionRepository;
import com.qpss.entity.Session;
import com.qpss.entity.Subject;
import com.qpss.document.parser.HeaderMetadataExtractor;
import com.qpss.document.model.HeaderMetadata;
import com.qpss.document.model.QuestionParseResult;
import com.qpss.dto.BulkUploadResult;
import com.qpss.util.RegexPatterns;
import com.qpss.util.ChecksumUtil;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

@Service
@RequiredArgsConstructor
public class BulkImportOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(BulkImportOrchestrator.class);

    private final HeaderMetadataExtractor metadataExtractor;
    private final QuestionParserService parserService;
    private final SubjectService subjectService;
    private final SessionService sessionService;
    private final QuestionBankImportRepository importRepo;
    private final SourceDocumentRepository sourceDocumentRepo;
    private final SourceDocumentStorageService storageService;
    private final QuestionRepository questionRepo;

    public BulkUploadResult processUpload(MultipartFile[] files) {
        BulkUploadResult result = BulkUploadResult.builder()
                .totalFiles(files.length)
                .errors(new ArrayList<>())
                .subjects(new ArrayList<>())
                .build();

        // Group files by subject extracted from their headers
        Map<String, List<FileParseHolder>> subjectGroups = new HashMap<>();
        Map<String, String> codeToName = new HashMap<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            String originalName = file.getOriginalFilename();
            if (originalName == null || !originalName.toLowerCase().endsWith(".docx")) {
                result.getErrors().add("Skipped non-DOCX file: " + originalName);
                continue;
            }

            try {
                // Extract header metadata for subject code/name
                String subjectCode = null;
                String subjectName = null;

                try (InputStream is = file.getInputStream()) {
                    XWPFDocument doc = new XWPFDocument(is);
                    HeaderMetadata metadata = metadataExtractor.extract(doc);
                    doc.close();

                    if (metadata.getSubjectCodeTitle() != null) {
                        String codeTitle = metadata.getSubjectCodeTitle();
                        Matcher m = RegexPatterns.SUBJECT_CODE_PATTERN.matcher(codeTitle);
                        if (m.find()) {
                            subjectCode = m.group(1);
                            // Subject name is everything after the code (strip " - " separator)
                            String afterCode = codeTitle.substring(m.end()).trim();
                            if (afterCode.startsWith("-") || afterCode.startsWith("–")) {
                                afterCode = afterCode.substring(1).trim();
                            }
                            subjectName = afterCode.isEmpty() ? null : afterCode;
                        } else {
                            // Try to use the whole line as the name and extract code from filename
                            subjectName = codeTitle;
                            Matcher fm = RegexPatterns.SUBJECT_CODE_PATTERN.matcher(originalName);
                            if (fm.find()) {
                                subjectCode = fm.group(1);
                            }
                        }
                    }

                    // Fallback: try to extract code from filename
                    if (subjectCode == null) {
                        Matcher fm = RegexPatterns.SUBJECT_CODE_PATTERN.matcher(originalName);
                        if (fm.find()) {
                            subjectCode = fm.group(1);
                        }
                    }
                }

                // Default grouping key if nothing found
                if (subjectCode == null) {
                    subjectCode = "UNKNOWN";
                    if (subjectName == null) {
                        subjectName = "Ungrouped Questions";
                    }
                }

                // Parse the questions
                QuestionParseResult parseResult = parserService.parseDocx(file);

                if (parseResult.getValidQuestions().isEmpty()) {
                    result.getErrors().add(originalName + ": No valid questions found");
                    continue;
                }

                // Save temp file for storage
                Path tempFile = Files.createTempFile("qpss_bulk_", ".docx");
                file.transferTo(tempFile.toFile());

                String checksum = ChecksumUtil.calculateChecksum(tempFile);

                FileParseHolder holder = new FileParseHolder();
                holder.originalName = originalName;
                holder.parseResult = parseResult;
                holder.tempFilePath = tempFile.toString();
                holder.checksum = checksum;
                holder.contentType = file.getContentType();
                holder.size = file.getSize();

                subjectGroups.computeIfAbsent(subjectCode, k -> new ArrayList<>()).add(holder);
                if (subjectName != null) {
                    codeToName.putIfAbsent(subjectCode, subjectName);
                }

            } catch (Exception e) {
                log.error("Failed to process file: {}", originalName, e);
                result.getErrors().add(originalName + ": " + e.getMessage());
            }
        }

        // Now process each subject group
        for (Map.Entry<String, List<FileParseHolder>> entry : subjectGroups.entrySet()) {
            String code = entry.getKey();
            String name = codeToName.getOrDefault(code, code);
            List<FileParseHolder> holders = entry.getValue();

            try {
                // Find or create the subject
                Subject subject = subjectService.findOrCreate(code, name);

                // Create a new session for this bulk import
                Session session = sessionService.create(subject.getId());

                // Create import batch
                QuestionBankImport importBatch = QuestionBankImport.builder()
                        .subjectId(subject.getId())
                        .sessionId(session.getId())
                        .build();
                importBatch = importRepo.save(importBatch);

                int questionsImported = 0;

                // Pre-fetch existing questions for deduplication
                List<Question> existingQuestions = questionRepo.findBySubjectIdOrderByUnitAscSerialNoAsc(subject.getId());
                java.util.Set<String> seenContents = new java.util.HashSet<>();
                for (Question eq : existingQuestions) {
                    seenContents.add(normalizeForDuplicateCheck(eq.getQuestionContent()));
                }

                for (FileParseHolder holder : holders) {
                    try {
                        // Check for duplicates
                        if (sourceDocumentRepo.existsByChecksumAndImportBatch_SessionId(
                                holder.checksum, session.getId())) {
                            continue;
                        }

                        // Store the document
                        String storedFileName = storageService.storeDocument(holder.tempFilePath, ".docx");

                        SourceDocument doc = SourceDocument.builder()
                                .importBatch(importBatch)
                                .originalFileName(holder.originalName)
                                .storedFileName(storedFileName)
                                .fileExtension(".docx")
                                .contentType(holder.contentType)
                                .fileSize(holder.size)
                                .checksum(holder.checksum)
                                .build();
                        doc = sourceDocumentRepo.save(doc);

                        // Save questions
                        List<Question> questions = parserService.toQuestions(
                                holder.parseResult.getValidQuestions(),
                                subject.getId(),
                                session.getId(),
                                doc.getId(),
                                holder.originalName
                        );
                        
                        List<Question> toSave = new ArrayList<>();
                        for (Question q : questions) {
                            String norm = normalizeForDuplicateCheck(q.getQuestionContent());
                            if (seenContents.add(norm)) {
                                toSave.add(q);
                            }
                        }

                        if (!toSave.isEmpty()) {
                            questionRepo.saveAll(toSave);
                            questionsImported += toSave.size();
                        }
                        result.setProcessedFiles(result.getProcessedFiles() + 1);

                    } catch (Exception e) {
                        log.error("Failed to import file {} for subject {}", holder.originalName, code, e);
                        result.getErrors().add(holder.originalName + ": " + e.getMessage());
                    } finally {
                        // Clean up temp file
                        try { Files.deleteIfExists(Path.of(holder.tempFilePath)); } catch (IOException ignored) {}
                    }
                }

                result.setTotalQuestions(result.getTotalQuestions() + questionsImported);

                if (questionsImported > 0) {
                    result.getSubjects().add(BulkUploadResult.SubjectSummary.builder()
                            .subjectId(subject.getId())
                            .code(subject.getCode())
                            .name(subject.getName())
                            .filesProcessed(holders.size())
                            .questionsImported(questionsImported)
                            .build());
                }

                if (!"UNKNOWN".equals(code)) {
                    result.setSubjectsCreated(result.getSubjectsCreated() + 1);
                }

            } catch (Exception e) {
                log.error("Failed to process subject group: {}", code, e);
                result.getErrors().add("Subject " + code + ": " + e.getMessage());
                // Clean up temp files for this group
                for (FileParseHolder h : holders) {
                    try { Files.deleteIfExists(Path.of(h.tempFilePath)); } catch (IOException ignored) {}
                }
            }
        }

        return result;
    }

    private String normalizeForDuplicateCheck(String content) {
        if (content == null) return "";
        String stripped = content.replaceAll("<[^>]*>", " ");
        stripped = stripped.replaceAll("[^a-zA-Z0-9]", "");
        return stripped.toLowerCase();
    }

    private static class FileParseHolder {
        String originalName;
        QuestionParseResult parseResult;
        String tempFilePath;
        String checksum;
        String contentType;
        long size;
    }
}
