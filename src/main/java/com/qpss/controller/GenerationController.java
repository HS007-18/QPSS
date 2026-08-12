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
    private final DocxRendererService docxRendererService;
    private final ExamConfigService examConfigService;

    @GetMapping("/preview")
    @ResponseBody
    public com.qpss.service.distribution.DistributionPlan preview(@RequestParam String examType, 
                                                                   @RequestParam(defaultValue = "FORMAT_1") String format) {
        return examConfigService.getDistributionPlan(examType, format);
    }

    @PostMapping
    public String generate(@PathVariable Long sessionId,
                            @RequestParam String examType,
                            @RequestParam(defaultValue = "1") int numSets,
                            @RequestParam(defaultValue = "FORMAT_1") String format,
                            Model model,
                            RedirectAttributes redirect) {
        var session = sessionService.findById(sessionId);
        var result = generationService.generate(
                examType, session.getSubjectId(), sessionId, numSets, format);

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

    @PostMapping("/{paperId}/swap")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> swapQuestion(@PathVariable Long sessionId, @PathVariable Long paperId, @RequestParam Long oldQuestionId) {
        try {
            com.qpss.model.Question newQuestion = generationService.swapQuestion(paperId, oldQuestionId);
            return org.springframework.http.ResponseEntity.ok().body("{\"status\":\"success\", \"newId\": " + newQuestion.getId() + ", \"newContent\": \"" + newQuestion.getQuestionContent().replace("\"", "\\\"").replace("\n", "\\n") + "\"}");
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/export/{paperId}")
    public org.springframework.http.ResponseEntity<byte[]> exportDocx(@PathVariable Long paperId) {
        var paper = generationService.getPaperById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paper id"));
        
        byte[] docxBytes = docxRendererService.exportPaperToDocx(paper);
        
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDispositionFormData("attachment", "QuestionPaper_" + paperId + ".docx");
        
        return new org.springframework.http.ResponseEntity<>(docxBytes, headers, org.springframework.http.HttpStatus.OK);
    }
}
