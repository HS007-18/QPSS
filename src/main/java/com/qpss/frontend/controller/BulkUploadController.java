package com.qpss.frontend.controller;
import com.qpss.backend.questionbank.QuestionBankImport;
import com.qpss.backend.questionbank.QuestionBankImportRepository;
import com.qpss.backend.questionbank.QuestionParserService;
import com.qpss.backend.questionbank.QuestionRepository;
import com.qpss.backend.questionbank.Question;
import com.qpss.backend.questionbank.SourceDocument;
import com.qpss.backend.questionbank.SourceDocumentRepository;
import com.qpss.backend.questionbank.SourceDocumentStorageService;
import com.qpss.backend.session.Session;
import com.qpss.backend.session.SessionService;
import com.qpss.backend.subject.Subject;
import com.qpss.backend.subject.SubjectService;
import com.qpss.documentextraction.extractor.HeaderMetadataExtractor;
import com.qpss.documentextraction.model.HeaderMetadata;
import com.qpss.documentextraction.model.QuestionParseResult;
import com.qpss.frontend.dto.BulkUploadResult;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
public class BulkUploadController {

    private static final Logger log = LoggerFactory.getLogger(BulkUploadController.class);

    private final HeaderMetadataExtractor metadataExtractor;
    private final QuestionParserService parserService;
    private final SubjectService subjectService;
    private final SessionService sessionService;
    private final QuestionBankImportRepository importRepo;
    private final SourceDocumentRepository sourceDocumentRepo;
    private final SourceDocumentStorageService storageService;
    private final QuestionRepository questionRepo;

    // Pattern to extract subject code like ME3491, CS3351, etc.
    private static final Pattern SUBJECT_CODE_PATTERN =
            Pattern.compile("\\b(\\d{2}[A-Z]{2,4}\\d{2,3})\\b");

    @PostMapping("/upload-bulk")
    public String bulkUpload(@RequestParam("files") MultipartFile[] files,
                             RedirectAttributes redirect) {
        if (files == null || files.length == 0) {
            redirect.addFlashAttribute("error", "No files selected for upload.");
            return "redirect:/";
        }

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
                        Matcher m = SUBJECT_CODE_PATTERN.matcher(codeTitle);
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
                            Matcher fm = SUBJECT_CODE_PATTERN.matcher(originalName);
                            if (fm.find()) {
                                subjectCode = fm.group(1);
                            }
                        }
                    }

                    // Fallback: try to extract code from filename
                    if (subjectCode == null) {
                        Matcher fm = SUBJECT_CODE_PATTERN.matcher(originalName);
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

                String checksum = calculateChecksum(tempFile);

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

        // Build success message
        StringBuilder message = new StringBuilder();
        message.append("Bulk upload complete: ");
        message.append(result.getProcessedFiles()).append("/").append(result.getTotalFiles()).append(" files processed, ");
        message.append(result.getTotalQuestions()).append(" questions imported across ");
        message.append(result.getSubjects().size()).append(" subject(s).");

        if (!result.getErrors().isEmpty()) {
            message.append(" (").append(result.getErrors().size()).append(" error(s))");
        }

        redirect.addFlashAttribute("message", message.toString());
        if (!result.getErrors().isEmpty()) {
            redirect.addFlashAttribute("uploadErrors", result.getErrors());
        }
        redirect.addFlashAttribute("bulkResult", result);

        return "redirect:/";
    }

    private String calculateChecksum(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream is = Files.newInputStream(file)) {
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
                if (hex.length() == 1) hexString.append('0');
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

    private static class FileParseHolder {
        String originalName;
        QuestionParseResult parseResult;
        String tempFilePath;
        String checksum;
        String contentType;
        long size;
    }
}
