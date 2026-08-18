package com.qpss.session.controller;

import com.qpss.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/subjects/{subjectId}")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/open")
    public String open(@PathVariable Long subjectId) {
        var session = sessionService.create(subjectId);
        return "redirect:/sessions/" + session.getId();
    }
}