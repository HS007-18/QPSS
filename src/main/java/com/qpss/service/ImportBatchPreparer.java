package com.qpss.service;

import com.qpss.dto.PendingUploadSession;
import com.qpss.document.model.QuestionParseResult;
import com.qpss.util.ChecksumUtil;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImportBatchPreparer {

    private final QuestionParserService parserService;

    public ImportBatchPreparer(QuestionParserService parserService) {
        this.parserService = parserService;
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

            String checksum = ChecksumUtil.calculateChecksum(file);
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
}
