package com.qpss.frontend.controller;
import com.qpss.backend.subject.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
@Controller
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("subjects", subjectService.findAll());
        return "subject/dashboard";
    }

    @PostMapping("/subjects")
    public String create(@RequestParam String name) {
        subjectService.create(name);
        return "redirect:/";
    }

    @PostMapping("/subjects/{id}/delete")
    public String delete(@PathVariable Long id) {
        subjectService.delete(id);
        return "redirect:/";
    }
}
