package com.qpss.generation.controller;

import com.qpss.domain.ExamType;
import com.qpss.questionbank.model.Question;
import com.qpss.questionbank.service.QuestionContentSanitizer;
import com.qpss.generation.distribution.DistributionPlan;
import com.qpss.generation.renderer.DocxRendererService;
import com.qpss.examconfig.service.ExamConfigService;
import com.qpss.generation.service.PaperGenerationService;
import com.qpss.session.service.SessionService;
import com.qpss.subject.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/sessions/{sessionId}/generate")
public class GenerationController {

    private final PaperGenerationService generationService;
    private final SessionService sessionService;
    private final SubjectService subjectService;
    private final DocxRendererService docxRendererService;
    private final ExamConfigService examConfigService;
    private final QuestionContentSanitizer contentSanitizer;

    @GetMapping("/preview")
    @ResponseBody
    public DistributionPlan preview(@RequestParam String examType,
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
        if (numSets < 1 || numSets > 10) {
            throw new IllegalArgumentException("Number of sets must be between 1 and 10.");
        }
        ExamType.from(examType);
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
        model.addAttribute("format", format);
        return "generation/review";
    }

    @PostMapping("/{paperId}/finalize")
    public String finalize(@PathVariable Long sessionId, @PathVariable Long paperId, Model model) {
        generationService.finalizePaper(paperId);
        model.addAttribute("session", sessionService.findById(sessionId));
        model.addAttribute("paperId", paperId);
        return "generation/finalized";
    }

    @PostMapping("/{paperId}/swap")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> swapQuestion(@PathVariable Long sessionId,
                                                            @PathVariable Long paperId,
                                                            @RequestParam Long oldQuestionId) {
        try {
            Question newQuestion = generationService.swapQuestion(paperId, oldQuestionId);
            Map<String, Object> body = new HashMap<>();
            body.put("status", "success");
            body.put("newId", newQuestion.getId());
            body.put("newContent", contentSanitizer.sanitize(newQuestion.getQuestionContent()));
            body.put("newMarks", newQuestion.getMarks());
            body.put("newMarksSplit", newQuestion.getMarksSplit());
            body.put("newUnit", newQuestion.getUnit());
            body.put("newRbt", newQuestion.getRbt());
            body.put("newQuestionType", newQuestion.getQuestionType());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            Map<String, Object> body = new HashMap<>();
            body.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(body);
        }
    }

    @GetMapping("/export/{paperId}")
    public ResponseEntity<byte[]> exportDocx(@PathVariable Long paperId) {
        var paper = generationService.getPaperById(paperId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paper id"));

        byte[] docxBytes = docxRendererService.exportPaperToDocx(paper);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDispositionFormData("attachment", "QuestionPaper_" + paperId + ".docx");

        return new ResponseEntity<>(docxBytes, headers, HttpStatus.OK);
    }
}