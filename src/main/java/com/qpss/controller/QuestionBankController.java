package com.qpss.controller;

import com.qpss.model.Question;
import com.qpss.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.qpss.repository.QuestionRepository;
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
            
            // Check if any question is incomplete
            boolean needsFix = pendingSession.getFiles().stream()
                    .flatMap(f -> f.getParseResult().getValidQuestions().stream())
                    .anyMatch(q -> !q.isComplete());

            if (needsFix) {
                httpSession.setAttribute("pendingUpload", pendingSession);
                return "redirect:/sessions/" + sessionId + "/upload/fix";
            }

            // If all complete, commit immediately
            var result = importService.commitImportBatch(pendingSession);
            if (result.isSuccessful()) {
                redirect.addFlashAttribute("message", "Uploaded " + result.getQuestionsParsed() + " questions from " + files.length + " file(s)");
            } else {
                redirect.addFlashAttribute("error", "Upload failed due to parsing errors:\n" + String.join("\n", result.getParsingErrors()));
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/sessions/" + sessionId;
    }

    @GetMapping("/upload/fix")
    public String showFixForm(@PathVariable Long sessionId, jakarta.servlet.http.HttpSession httpSession, Model model) {
        PendingUploadSession pendingSession = (PendingUploadSession) httpSession.getAttribute("pendingUpload");
        if (pendingSession == null) {
            return "redirect:/sessions/" + sessionId;
        }

        // Find the first incomplete question
        for (PendingUploadSession.FileImportHolder fileHolder : pendingSession.getFiles()) {
            for (com.qpss.service.parser.ParsedQuestion q : fileHolder.getParseResult().getValidQuestions()) {
                if (!q.isComplete()) {
                    model.addAttribute("file", fileHolder.getOriginalName());
                    model.addAttribute("question", q);
                    model.addAttribute("session", sessionService.findById(sessionId));
                    return "fix-question";
                }
            }
        }

        // If none found, commit!
        try {
            var result = importService.commitImportBatch(pendingSession);
            httpSession.removeAttribute("pendingUpload");
            model.addAttribute("message", "Uploaded " + result.getQuestionsParsed() + " questions successfully.");
        } catch (Exception e) {
            model.addAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/sessions/" + sessionId;
    }

    @PostMapping("/upload/fix")
    public String submitFix(@PathVariable Long sessionId,
                            @RequestParam String file,
                            @RequestParam int serialNo,
                            @RequestParam(required = false) Integer unit,
                            @RequestParam(required = false) String co,
                            @RequestParam(required = false) Integer marks,
                            @RequestParam(required = false) Integer t,
                            @RequestParam(required = false) String questionContent,
                            jakarta.servlet.http.HttpSession httpSession) {
        PendingUploadSession pendingSession = (PendingUploadSession) httpSession.getAttribute("pendingUpload");
        if (pendingSession != null) {
            for (PendingUploadSession.FileImportHolder fileHolder : pendingSession.getFiles()) {
                if (fileHolder.getOriginalName().equals(file)) {
                    for (com.qpss.service.parser.ParsedQuestion q : fileHolder.getParseResult().getValidQuestions()) {
                        if (q.getSerialNo().equals(serialNo)) {
                            if (co != null && !co.trim().isEmpty()) {
                                q.setCo(co);
                                // Automatically derive unit from CO if not already set by user
                                if (unit == null && q.getUnit() == null) {
                                    try {
                                        q.setUnit(Integer.parseInt(co.replaceAll("[^0-9]", "")));
                                    } catch (NumberFormatException e) {
                                        // ignore
                                    }
                                }
                            }
                            if (unit != null) q.setUnit(unit);
                            if (marks != null) q.setMarks(marks);
                            if (t != null) q.setT(t);
                            if (questionContent != null && !questionContent.trim().isEmpty()) q.setQuestionContent(questionContent);
                            break;
                        }
                    }
                }
            }
        }
        return "redirect:/sessions/" + sessionId + "/upload/fix";
    }

    @PostMapping("/questions")
    public String addQuestion(@PathVariable Long sessionId,
                               @RequestParam int unit,
                               @RequestParam String co,
                               @RequestParam int marks,
                               @RequestParam int t,
                               @RequestParam String content,
                               RedirectAttributes redirect) {
        var session = sessionService.findById(sessionId);
        bankService.addQuestion(session.getSubjectId(), sessionId, unit, co, marks, t, null, content);
        redirect.addFlashAttribute("message", "Question added");
        return "redirect:/sessions/" + sessionId;
    }
}
