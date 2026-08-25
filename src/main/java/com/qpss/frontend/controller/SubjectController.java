package com.qpss.frontend.controller;
import com.qpss.backend.subject.Subject;
import com.qpss.backend.subject.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Controller
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping("/")
    public String dashboard(Model model) {
        List<Subject> subjects = subjectService.findAll();
        model.addAttribute("subjects", subjects);

        // Build stats map for each subject
        Map<Long, Long> questionCounts = new HashMap<>();
        Map<Long, Long> importCounts = new HashMap<>();
        for (Subject s : subjects) {
            questionCounts.put(s.getId(), subjectService.getQuestionCount(s.getId()));
            importCounts.put(s.getId(), subjectService.getImportCount(s.getId()));
        }
        model.addAttribute("questionCounts", questionCounts);
        model.addAttribute("importCounts", importCounts);

        return "subject/dashboard";
    }

    @GetMapping("/api/subjects/search")
    @ResponseBody
    public List<Subject> searchSubjects(@RequestParam(defaultValue = "") String q) {
        return subjectService.search(q);
    }

    @PostMapping("/subjects")
    public String create(@RequestParam String name,
                         @RequestParam(required = false) String code) {
        if (code != null && !code.isBlank()) {
            subjectService.findOrCreate(code, name);
        } else {
            subjectService.create(name);
        }
        return "redirect:/";
    }

    @PostMapping("/subjects/{id}/delete")
    public String delete(@PathVariable Long id) {
        subjectService.delete(id);
        return "redirect:/";
    }
}
