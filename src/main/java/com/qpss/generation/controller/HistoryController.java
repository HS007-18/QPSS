package com.qpss.generation.controller;

import com.qpss.generation.repository.GeneratedPaperRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HistoryController {

    private final GeneratedPaperRepository paperRepository;

    @GetMapping("/history")
    public String getHistory(Model model) {
        model.addAttribute("papers", paperRepository.findByIsFinalTrue());
        return "generation/history";
    }
}