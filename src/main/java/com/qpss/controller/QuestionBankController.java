package com.qpss.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.qpss.document.model.QuestionConstants;
import com.qpss.service.QuestionBankService;
import com.qpss.service.SessionService;
import com.qpss.service.SubjectService;
import com.qpss.util.QuestionContentSanitizer;

import lombok.RequiredArgsConstructor;
@Controller
@RequiredArgsConstructor
@RequestMapping("/sessions/{sessionId}")
public class QuestionBankController {

    private final QuestionBankService bankService;
    private final SessionService sessionService;
    private final SubjectService subjectService;
    private final QuestionContentSanitizer contentSanitizer;

    @GetMapping
    public String session(@PathVariable Long sessionId, Model model) {
        var session = sessionService.findById(sessionId);
        var questions = bankService.getQuestionsBySubjectId(session.getSubjectId());
        questions.forEach(q -> q.setQuestionContent(contentSanitizer.sanitize(q.getQuestionContent())));
        model.addAttribute("session", session);
        model.addAttribute("subject", subjectService.findById(session.getSubjectId()));
        model.addAttribute("questions", questions);
        return "session/session";
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
        if (unit < 1 || unit > 5) {
            throw new IllegalArgumentException("Unit must be between 1 and 5.");
        }
        if (!QuestionConstants.MARK_VALUES.contains(marks)) {
            throw new IllegalArgumentException("Marks must be one of " + QuestionConstants.MARK_VALUES + ".");
        }
        if (!QuestionConstants.T_VALUES.contains(t)) {
            throw new IllegalArgumentException("T must be 1 or 2.");
        }
        String normalizedRbt = rbt.trim().toUpperCase();
        if (!QuestionConstants.RBT_VALUES.contains(normalizedRbt)) {
            throw new IllegalArgumentException("RBT must be one of " + QuestionConstants.RBT_VALUES + ".");
        }
        if (!co.matches("(?i)CO\\d+")) {
            throw new IllegalArgumentException("CO must be in the form CO1, CO2, ...");
        }

        var session = sessionService.findById(sessionId);
        bankService.addQuestion(session.getSubjectId(), sessionId, unit, normalizedRbt, co.toUpperCase(), marks, t,
                null, contentSanitizer.sanitize(content));
        redirect.addFlashAttribute("message", "Question added");
        return "redirect:/sessions/" + sessionId;
    }
}