package com.qpss.controller;

import com.qpss.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/sessions/{sessionId}/generate")
public class GenerationController {

    private final PaperGenerationService generationService;
    private final SessionService sessionService;
    private final SubjectService subjectService;
    private final PdfExportService pdfExportService;

    @PostMapping
    public String generate(@PathVariable Long sessionId,
                            @RequestParam String examType,
                            @RequestParam(defaultValue = "1") int numSets,
                            Model model,
                            RedirectAttributes redirect) {
        var session = sessionService.findById(sessionId);
        var result = generationService.generate(
                examType, session.getSubjectId(), sessionId, numSets);

        if (!result.isSuccessful()) {
            redirect.addFlashAttribute("shortages", result.getShortages());
            return "redirect:/sessions/" + sessionId;
        }

        model.addAttribute("session", session);
        model.addAttribute("subject", subjectService.findById(session.getSubjectId()));
        model.addAttribute("result", result);
        model.addAttribute("examType", examType);
        return "review";
    }

    @PostMapping("/{paperId}/finalize")
    public String finalize(@PathVariable Long sessionId, @PathVariable Long paperId, Model model) {
        generationService.finalizePaper(paperId);
        model.addAttribute("session", sessionService.findById(sessionId));
        model.addAttribute("paperId", paperId);
        return "finalized";
    }


    @GetMapping("/export/{paperId}")
    public org.springframework.http.ResponseEntity<byte[]> exportPdf(@PathVariable Long paperId) {
        // We might want to verify paper belongs to session here
        var paper = generationService.getPaperById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paper id"));
        
        byte[] pdfBytes = pdfExportService.exportPaperToPdf(paper);
        
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "QuestionPaper_" + paperId + ".pdf");
        
        return new org.springframework.http.ResponseEntity<>(pdfBytes, headers, org.springframework.http.HttpStatus.OK);
    }
}
