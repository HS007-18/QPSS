package com.qpss.controller;

import com.qpss.service.SessionService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/subjects/{subjectId}")
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public String subject(@PathVariable Long subjectId) {
        var session = sessionService.create(subjectId);
        return "redirect:/sessions/" + session.getId();
    }
}
