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

    private final com.qpss.repository.QuestionRepository questionRepository;

    @GetMapping
    public String session(@PathVariable Long sessionId, Model model) {
        var session = sessionService.findById(sessionId);
        var questions = bankService.getQuestionsBySubjectId(session.getSubjectId());
        questions.forEach(q -> q.setQuestionContent(contentSanitizer.sanitize(q.getQuestionContent())));
        
        // Fetch dynamic unit/topic stats for FORMAT_3 UI
        java.util.Map<String, Integer> statsMap = new java.util.HashMap<>();
        for (com.qpss.entity.Question q : questions) {
            if (q.getMarks() != null && q.getMarks() == 2) {
                Integer identifier = q.getTopic() != null ? q.getTopic() : q.getT();
                if (identifier != null) {
                    String key = q.getUnit() + "_" + identifier;
                    statsMap.put(key, statsMap.getOrDefault(key, 0) + 1);
                }
            }
        }
        java.util.List<java.util.Map<String, Object>> topicStats = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Integer> entry : statsMap.entrySet()) {
            String[] parts = entry.getKey().split("_");
            java.util.Map<String, Object> stat = new java.util.HashMap<>();
            stat.put("unit", Integer.parseInt(parts[0]));
            stat.put("topic", Integer.parseInt(parts[1]));
            stat.put("count", entry.getValue());
            topicStats.add(stat);
        }
        topicStats.sort((a, b) -> {
            int unitCmp = Integer.compare((Integer) a.get("unit"), (Integer) b.get("unit"));
            if (unitCmp != 0) return unitCmp;
            return Integer.compare((Integer) a.get("topic"), (Integer) b.get("topic"));
        });
        
        model.addAttribute("session", session);
        model.addAttribute("subject", subjectService.findById(session.getSubjectId()));
        model.addAttribute("questions", questions);
        model.addAttribute("topicStats", topicStats);
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