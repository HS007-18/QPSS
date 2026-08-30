package com.qpss.controller;

import com.qpss.dto.BulkUploadResult;
import com.qpss.service.BulkImportOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class BulkUploadController {


    private final BulkImportOrchestrator bulkImportOrchestrator;

    @PostMapping("/upload-bulk")
    public String bulkUpload(@RequestParam("files") MultipartFile[] files,
                             RedirectAttributes redirect) {
        if (files == null || files.length == 0) {
            redirect.addFlashAttribute("error", "No files selected for upload.");
            return "redirect:/";
        }

        BulkUploadResult result = bulkImportOrchestrator.processUpload(files);

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
}
