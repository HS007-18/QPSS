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
import com.qpss.service.SubjectService;
import com.qpss.util.QuestionContentSanitizer;

import lombok.RequiredArgsConstructor;
@Controller
@RequiredArgsConstructor
@RequestMapping("/subjects/{subjectId}")
public class QuestionBankController {

    private final QuestionBankService bankService;
    private final SubjectService subjectService;
    private final QuestionContentSanitizer contentSanitizer;

    private final com.qpss.repository.QuestionRepository questionRepository;

    @GetMapping
    public String viewSubject(@PathVariable Long subjectId, Model model) {
        var subject = subjectService.findById(subjectId);
        var questions = bankService.getQuestionsBySubjectId(subjectId);
        questions.forEach(q -> q.setQuestionContent(contentSanitizer.sanitize(q.getQuestionContent())));
        
        var stats2M = computeStatsForMarks(questions, 2);
        var stats16M = computeStatsForMarks(questions, 16);
        var stats20M = computeStatsForMarks(questions, 20);
        
        model.addAttribute("subject", subject);
        model.addAttribute("questions", questions);
        model.addAttribute("topicStats", stats2M);
        model.addAttribute("stats2M", stats2M);
        model.addAttribute("stats16M", stats16M);
        model.addAttribute("stats20M", stats20M);
        return "subject/detail";
    }

    private java.util.List<java.util.Map<String, Object>> computeStatsForMarks(java.util.List<com.qpss.entity.Question> questions, int marks) {
        java.util.Map<String, Integer> statsMap = new java.util.HashMap<>();
        for (com.qpss.entity.Question q : questions) {
            if (q.getMarks() != null && q.getMarks() == marks) {
                Integer identifier = q.getTopic() != null ? q.getTopic() : q.getT();
                if (identifier != null) {
                    String key = q.getUnit() + "_" + identifier;
                    statsMap.put(key, statsMap.getOrDefault(key, 0) + 1);
                }
            }
        }
        java.util.List<java.util.Map<String, Object>> statsList = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Integer> entry : statsMap.entrySet()) {
            String[] parts = entry.getKey().split("_");
            java.util.Map<String, Object> stat = new java.util.HashMap<>();
            stat.put("unit", Integer.parseInt(parts[0]));
            stat.put("topic", Integer.parseInt(parts[1]));
            stat.put("count", entry.getValue());
            statsList.add(stat);
        }
        statsList.sort((a, b) -> {
            int unitCmp = Integer.compare((Integer) a.get("unit"), (Integer) b.get("unit"));
            if (unitCmp != 0) return unitCmp;
            return Integer.compare((Integer) a.get("topic"), (Integer) b.get("topic"));
        });
        return statsList;
    }

    @PostMapping("/questions")
    public String addQuestion(@PathVariable Long subjectId,
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

        bankService.addQuestion(subjectId, unit, normalizedRbt, co.toUpperCase(), marks, t,
                null, contentSanitizer.sanitize(content));
        redirect.addFlashAttribute("message", "Question added");
        return "redirect:/subjects/" + subjectId;
    }
}