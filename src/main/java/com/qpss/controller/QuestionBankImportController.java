package com.qpss.controller;

import com.qpss.model.QuestionBankImport;
import com.qpss.service.QuestionBankImportResult;
import com.qpss.service.QuestionBankImportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/import")
public class QuestionBankImportController {

    private final QuestionBankImportService importService;

    public QuestionBankImportController(QuestionBankImportService importService) {
        this.importService = importService;
    }

    @PostMapping("/question-bank")
    public ResponseEntity<?> importQuestionBank(
            @RequestParam("subjectId") Long subjectId,
            @RequestParam("sessionId") Long sessionId,
            @RequestParam("files") MultipartFile[] files) {
        
        try {
            List<MultipartFile> fileList = Arrays.asList(files);
            var pendingSession = importService.prepareImportBatch(subjectId, sessionId, fileList);
            
            boolean needsFix = pendingSession.getFiles().stream()
                    .flatMap(f -> f.getParseResult().getValidQuestions().stream())
                    .anyMatch(q -> !q.isComplete());

            if (needsFix) {
                return ResponseEntity.badRequest().body("Import failed: Some questions are missing required data (Marks, CO, Unit, T, or Content). Interactive UI is required to fix this.");
            }

            QuestionBankImportResult result = importService.commitImportBatch(pendingSession);
            
            if (!result.isSuccessful()) {
                return ResponseEntity.badRequest().body("Import failed with parsing errors:\n" + String.join("\n", result.getParsingErrors()));
            }

            return ResponseEntity.ok().body(String.format("Import successful. Batch ID: %d, Questions parsed: %d", 
                    result.getImportBatch().getId(), result.getQuestionsParsed()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Import failed: " + e.getMessage());
        }
    }
}
