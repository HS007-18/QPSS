package com.qpss.controller;

import com.qpss.model.Question;
import com.qpss.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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

    @GetMapping
    public String session(@PathVariable Long sessionId, Model model) {
        var session = sessionService.findById(sessionId);
        model.addAttribute("session", session);
        model.addAttribute("subject", subjectService.findById(session.getSubjectId()));
        return "session";
    }

    @PostMapping("/upload")
    public String upload(@PathVariable Long sessionId,
                          @RequestParam("file") MultipartFile file,
                          RedirectAttributes redirect) {
        try {
            var session = sessionService.findById(sessionId);
            var result = importService.createImportBatch(session.getSubjectId(), sessionId, List.of(file));
            
            if (result.isSuccessful()) {
                redirect.addFlashAttribute("message",
                        "Uploaded " + result.getQuestionsParsed() + " questions from " + file.getOriginalFilename());
            } else {
                redirect.addFlashAttribute("error", "Upload failed due to parsing errors: " + String.join(", ", result.getParsingErrors()));
            }
        } catch (Exception e) {
            redirect.addFlashAttribute("error", "Upload failed: " + e.getMessage());
        }
        return "redirect:/sessions/" + sessionId;
    }

    @PostMapping("/questions")
    public String addQuestion(@PathVariable Long sessionId,
                               @RequestParam int unit,
                               @RequestParam String co,
                               @RequestParam int marks,
                               @RequestParam String content,
                               RedirectAttributes redirect) {
        var session = sessionService.findById(sessionId);
        bankService.addQuestion(session.getSubjectId(), sessionId, unit, co, marks, null, content);
        redirect.addFlashAttribute("message", "Question added");
        return "redirect:/sessions/" + sessionId;
    }
}
