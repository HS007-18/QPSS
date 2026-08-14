package com.qpss.controller;

import com.qpss.dto.FixQuestionCommand;
import com.qpss.dto.PendingUploadSession;
import com.qpss.model.Question;
import com.qpss.repository.QuestionRepository;
import com.qpss.service.QuestionBankImportService;
import com.qpss.service.QuestionBankService;
import com.qpss.service.QuestionParserService;
import com.qpss.service.SessionService;
import com.qpss.service.SubjectService;
import com.qpss.service.parser.ParsedQuestion;
import com.qpss.service.parser.QuestionConstants;
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

import java.util.Arrays;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/sessions/{sessionId}")
public class QuestionBankController {

    private final QuestionBankService bankService;
    private final QuestionParserService parserService;
    private final SessionService sessionService;
    private final SubjectService subjectService;
    private final QuestionBankImportService importService;
    private final QuestionRepository questionRepo;

    @GetMapping
    public String session(@PathVariable Long sessionId, Model model) {
        var session = sessionService.findById(sessionId);
        model.addAttribute("session", session);
        model.addAttribute("subject", subjectService.findById(session.getSubjectId()));
        model.addAttribute("questions", questionRepo.findBySessionIdOrderByUnitAscSerialNoAsc(sessionId));
        return "session";
    }

    @PostMapping("/upload")
    public String upload(@PathVariable Long sessionId,
                          @RequestParam("files") MultipartFile[] files,
                          jakarta.servlet.http.HttpSession httpSession,
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
                String message = "Uploaded " + result.getQuestionsParsed() + " questions from " + files.length + " file(s)";
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

    @GetMapping("/upload/fix")
    public String showFixForm(@PathVariable Long sessionId, jakarta.servlet.http.HttpSession httpSession,
                              Model model, RedirectAttributes redirect) {
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
                    return "fix-question";
                }
            }
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

    @PostMapping("/upload/fix")
    public String submitFix(@PathVariable Long sessionId, FixQuestionCommand command,
                            jakarta.servlet.http.HttpSession httpSession) {
        PendingUploadSession pendingSession = (PendingUploadSession) httpSession.getAttribute("pendingUpload");
        if (pendingSession != null) {
            for (PendingUploadSession.FileImportHolder fileHolder : pendingSession.getFiles()) {
                if (fileHolder.getOriginalName().equals(command.getFile())) {
                    for (ParsedQuestion q : fileHolder.getParseResult().getValidQuestions()) {
                        if (q.getSerialNo().equals(command.getSerialNo())) {
                            applyFix(q, command);
                            break;
                        }
                    }
                }
            }
        }
        return "redirect:/sessions/" + sessionId + "/upload/fix";
    }

    private void applyFix(ParsedQuestion q, FixQuestionCommand command) {
        if (hasText(command.getCo())) {
            q.setCo(command.getCo());
            if (command.getUnit() == null && q.getUnit() == null) {
                q.setUnit(extractUnitFromCo(command.getCo()));
            }
        }
        if (command.getUnit() != null && command.getUnit() > 0) {
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
            q.setQuestionContent(command.getQuestionContent());
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

    @PostMapping("/questions")
    public String addQuestion(@PathVariable Long sessionId,
                               @RequestParam int unit,
                               @RequestParam String rbt,
                               @RequestParam String co,
                               @RequestParam int marks,
                               @RequestParam int t,
                               @RequestParam String content,
                               RedirectAttributes redirect) {
        var session = sessionService.findById(sessionId);
        bankService.addQuestion(session.getSubjectId(), sessionId, unit, rbt, co, marks, t, null, content);
        redirect.addFlashAttribute("message", "Question added");
        return "redirect:/sessions/" + sessionId;
    }
}