package com.qpss.controller;
import com.qpss.domain.ExamType;
import com.qpss.entity.Question;
import com.qpss.util.QuestionContentSanitizer;
import com.qpss.domain.distribution.DistributionPlan;
import com.qpss.service.DocxRendererService;
import com.qpss.service.ExamConfigService;
import com.qpss.service.PaperGenerationService;
import com.qpss.service.SessionService;
import com.qpss.service.SubjectService;
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
    private final com.qpss.service.PartAOnlyGenerationService partAOnlyGenService;

    @GetMapping("/preview")
    @ResponseBody
    public DistributionPlan preview(@RequestParam String examType,
                                                                   @RequestParam(defaultValue = "FORMAT_1") String format) {
        return examConfigService.getDistributionPlan(ExamType.from(examType), com.qpss.domain.ExamFormat.from(format));
    }

    @PostMapping
    public String generate(@PathVariable Long sessionId,
                            @RequestParam String examType,
                            @RequestParam(defaultValue = "1") int numSets,
                            @RequestParam(defaultValue = "FORMAT_1") String format,
                            @RequestParam(defaultValue = "Three Hours") String duration,
                            @RequestParam Map<String, String> allParams,
                            Model model,
                            RedirectAttributes redirect) {
        if (numSets < 1 || numSets > 10) {
            throw new IllegalArgumentException("Number of sets must be between 1 and 10.");
        }
        ExamType.from(examType);
        var session = sessionService.findById(sessionId);
        PaperGenerationService.GenerationResult result;
        
        if ("FORMAT_3".equals(format)) {
            Map<String, Integer> topicCounts = new HashMap<>();
            allParams.forEach((k, v) -> {
                if (k.startsWith("topic_")) {
                    String key = k.substring("topic_".length());
                    try {
                        int count = Integer.parseInt(v);
                        if (count > 0) topicCounts.put(key, count);
                    } catch (NumberFormatException ignored) {}
                }
            });
            result = generationService.generatePartAOnly(examType, session.getSubjectId(), sessionId, numSets, duration, topicCounts, partAOnlyGenService);
        } else {
            result = generationService.generate(examType, session.getSubjectId(), sessionId, numSets, format, duration);
        }

        if (!result.isSuccessful()) {
            redirect.addFlashAttribute("shortages", result.getShortages());
            return "redirect:/sessions/" + sessionId;
        }

        result.getSets().forEach(set -> {
            set.getSectionA().forEach(q -> q.setQuestionContent(contentSanitizer.sanitize(q.getQuestionContent())));
            set.getSectionB().forEach(pair -> {
                pair.getChoiceA().setQuestionContent(contentSanitizer.sanitize(pair.getChoiceA().getQuestionContent()));
                pair.getChoiceB().setQuestionContent(contentSanitizer.sanitize(pair.getChoiceB().getQuestionContent()));
            });
        });

        model.addAttribute("session", session);
        model.addAttribute("subject", subjectService.findById(session.getSubjectId()));
        model.addAttribute("result", result);
        model.addAttribute("examType", examType);
        model.addAttribute("format", format);
        return "generation/review";
    }

    @PostMapping("/{paperId}/finalize")
    public String finalize(@PathVariable Long sessionId, @PathVariable Long paperId, Model model) {
        generationService.finalizePaper(paperId, sessionId);
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
            Question newQuestion = generationService.swapQuestion(paperId, oldQuestionId, sessionId);
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
    public ResponseEntity<byte[]> exportDocx(@PathVariable Long sessionId, @PathVariable Long paperId) {
        var paper = generationService.getPaperByIdAndSessionId(paperId, sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paper id or session mismatch"));

        byte[] docxBytes = docxRendererService.exportPaperToDocx(paper);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDispositionFormData("attachment", "QuestionPaper_" + paperId + ".docx");

        return new ResponseEntity<>(docxBytes, headers, HttpStatus.OK);
    }
}