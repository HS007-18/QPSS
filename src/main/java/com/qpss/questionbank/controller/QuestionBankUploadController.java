package com.qpss.questionbank.controller;

import com.qpss.questionbank.dto.FixQuestionCommand;
import com.qpss.questionbank.dto.PendingUploadSession;
import com.qpss.questionbank.parser.ParsedQuestion;
import com.qpss.questionbank.parser.QuestionConstants;
import com.qpss.questionbank.service.QuestionBankImportService;
import com.qpss.questionbank.service.QuestionContentSanitizer;
import com.qpss.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.Arrays;

@Controller
@RequiredArgsConstructor
@RequestMapping("/sessions/{sessionId}/upload")
public class QuestionBankUploadController {

    private final QuestionBankImportService importService;
    private final SessionService sessionService;
    private final QuestionContentSanitizer contentSanitizer;

    @PostMapping
    public String upload(@PathVariable Long sessionId,
                          @RequestParam("files") MultipartFile[] files,
                          HttpSession httpSession,
                          RedirectAttributes redirect) {
        try {
            var session = sessionService.findById(sessionId);
            PendingUploadSession pendingSession = importService.prepareImportBatch(session.getSubjectId(), sessionId, Arrays.asList(files));

            boolean needsFix = pendingSession.getFiles().stream()
                    .flatMap(f -> f.getParseResult().getValidQuestions().stream())
                    .anyMatch(q -> !q.isComplete());

            if (needsFix) {
                httpSession.setAttribute("pendingUpload", pendingSession);
                return "redirect:/sessions/" + sessionId + "/upload/fix";
            }

            var result = importService.commitImportBatch(pendingSession);
            if (result.isSuccessful()) {
                String message = "Uploaded " + result.getQuestionsParsed() + " questions from " + pendingSession.getFiles().size() + " file(s)";
                if (result.getSkippedDuplicates() > 0) {
                    message += " (" + result.getSkippedDuplicates() + " already in this session)";
                }
                redirect.addFlashAttribute("message", message);
            } else {
                redirect.addFlashAttribute("error", "Upload failed due to parsing errors:\n" + String.join("\n", result.getParsingErrors()));
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/sessions/" + sessionId;
    }

    @GetMapping("/fix")
    public String showFixForm(@PathVariable Long sessionId, HttpSession httpSession, Model model) {
        PendingUploadSession pendingSession = (PendingUploadSession) httpSession.getAttribute("pendingUpload");
        if (pendingSession == null) {
            return "redirect:/sessions/" + sessionId;
        }

        for (PendingUploadSession.FileImportHolder fileHolder : pendingSession.getFiles()) {
            for (ParsedQuestion q : fileHolder.getParseResult().getValidQuestions()) {
                if (!q.isComplete()) {
                    model.addAttribute("file", fileHolder.getOriginalName());
                    model.addAttribute("question", q);
                    model.addAttribute("session", sessionService.findById(sessionId));
                    return "questionbank/fix-question";
                }
            }
        }

        return "redirect:/sessions/" + sessionId;
    }

    @PostMapping("/fix")
    public String submitFix(@PathVariable Long sessionId, FixQuestionCommand command,
                            HttpSession httpSession, RedirectAttributes redirect) {
        PendingUploadSession pendingSession = (PendingUploadSession) httpSession.getAttribute("pendingUpload");
        if (pendingSession == null) {
            return "redirect:/sessions/" + sessionId;
        }

        boolean fixed = false;
        for (PendingUploadSession.FileImportHolder fileHolder : pendingSession.getFiles()) {
            if (fileHolder.getOriginalName().equals(command.getFile())) {
                for (ParsedQuestion q : fileHolder.getParseResult().getValidQuestions()) {
                    if (q.getSerialNo().equals(command.getSerialNo())) {
                        applyFix(q, command);
                        fixed = true;
                        break;
                    }
                }
                break;
            }
        }

        if (!fixed) {
            redirect.addFlashAttribute("error", "Uploaded question not found. Please re-upload the file.");
            httpSession.removeAttribute("pendingUpload");
            return "redirect:/sessions/" + sessionId;
        }

        boolean allComplete = pendingSession.getFiles().stream()
                .flatMap(f -> f.getParseResult().getValidQuestions().stream())
                .allMatch(ParsedQuestion::isComplete);
        if (!allComplete) {
            return "redirect:/sessions/" + sessionId + "/upload/fix";
        }

        try {
            var result = importService.commitImportBatch(pendingSession);
            httpSession.removeAttribute("pendingUpload");
            if (result.isSuccessful()) {
                String message = "Uploaded " + result.getQuestionsParsed() + " questions successfully.";
                if (result.getSkippedDuplicates() > 0) {
                    message += " (" + result.getSkippedDuplicates() + " already in this session)";
                }
                redirect.addFlashAttribute("message", message);
            } else {
                redirect.addFlashAttribute("error", "Upload failed due to parsing errors:\n"
                        + String.join("\n", result.getParsingErrors()));
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/sessions/" + sessionId;
    }

    private void applyFix(ParsedQuestion q, FixQuestionCommand command) {
        if (hasText(command.getCo())) {
            String normalized = command.getCo().trim().toUpperCase();
            if (normalized.matches("CO\\d+")) {
                q.setCo(normalized);
                if (command.getUnit() == null && q.getUnit() == null) {
                    q.setUnit(extractUnitFromCo(normalized));
                }
            }
        }
        if (command.getUnit() != null && command.getUnit() >= 1 && command.getUnit() <= 5) {
            q.setUnit(command.getUnit());
        }
        if (command.getMarks() != null && QuestionConstants.MARK_VALUES.contains(command.getMarks())) {
            q.setMarks(command.getMarks());
        }
        if (command.getT() != null && QuestionConstants.T_VALUES.contains(command.getT())) {
            q.setT(command.getT());
        }
        if (hasText(command.getRbt())) {
            String normalized = command.getRbt().trim().toUpperCase();
            if (QuestionConstants.RBT_VALUES.contains(normalized)) {
                q.setRbt(normalized);
            }
        }
        if (hasText(command.getQuestionContent())) {
            q.setQuestionContent(contentSanitizer.sanitize(command.getQuestionContent()));
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Integer extractUnitFromCo(String co) {
        try {
            return Integer.parseInt(co.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}