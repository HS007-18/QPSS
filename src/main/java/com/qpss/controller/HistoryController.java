package com.qpss.controller;

import com.qpss.model.GeneratedPaper;
import com.qpss.repository.GeneratedPaperRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class HistoryController {

    private final GeneratedPaperRepository paperRepository;

    @GetMapping("/history")
    public String getHistory(Model model) {
        List<GeneratedPaper> finalizedPapers = paperRepository.findAll().stream()
                .filter(p -> Boolean.TRUE.equals(p.getIsFinal()))
                .collect(Collectors.toList());
        model.addAttribute("papers", finalizedPapers);
        return "history";
    }
}
