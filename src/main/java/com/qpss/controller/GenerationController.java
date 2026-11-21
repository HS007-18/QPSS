package com.qpss.controller;
import com.qpss.domain.ExamType;
import com.qpss.entity.Question;
import com.qpss.util.QuestionContentSanitizer;
import com.qpss.domain.distribution.DistributionPlan;
import com.qpss.service.DocxRendererService;
import com.qpss.service.ExamConfigService;
import com.qpss.service.PaperGenerationService;
import com.qpss.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
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
@RequestMapping("/subjects/{subjectId}/generate")
public class GenerationController {

    private final PaperGenerationService generationService;
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
    public String generate(@PathVariable Long subjectId,
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
        var subject = subjectService.findById(subjectId);
        PaperGenerationService.GenerationResult result;
        
        Map<String, Integer> counts2M = new HashMap<>();
        Map<String, Integer> counts16M = new HashMap<>();
        Map<String, Integer> counts20M = new HashMap<>();

        allParams.forEach((k, v) -> {
            try {
                int count = Integer.parseInt(v);
                if (count > 0) {
                    if (k.startsWith("count_2_")) {
                        counts2M.put(k.substring("count_2_".length()), count);
                    } else if (k.startsWith("topic_")) {
                        counts2M.put(k.substring("topic_".length()), count);
                    } else if (k.startsWith("count_16_")) {
                        counts16M.put(k.substring("count_16_".length()), count);
                    } else if (k.startsWith("count_20_")) {
                        counts20M.put(k.substring("count_20_".length()), count);
                    }
                }
            } catch (NumberFormatException ignored) {}
        });

        if ("FORMAT_3".equals(format)) {
            result = generationService.generatePartAOnly(examType, subjectId, numSets, duration, counts2M, partAOnlyGenService);
        } else if ("FORMAT_2".equals(format)) {
            if (!counts20M.isEmpty()) {
                result = generationService.generateDynamic20M(examType, subjectId, numSets, duration, counts20M, partAOnlyGenService);
            } else {
                result = generationService.generate(examType, subjectId, numSets, format, duration);
            }
        } else {
            // FORMAT_1
            if (!counts2M.isEmpty() || !counts16M.isEmpty()) {
                result = generationService.generateDynamicFormat1(examType, subjectId, numSets, duration, counts2M, counts16M, partAOnlyGenService);
            } else {
                result = generationService.generate(examType, subjectId, numSets, format, duration);
            }
        }

        if (!result.isSuccessful()) {
            redirect.addFlashAttribute("shortages", result.getShortages());
            return "redirect:/subjects/" + subjectId;
        }

        result.getSets().forEach(set -> {
            set.getSectionA().forEach(q -> q.setQuestionContent(contentSanitizer.sanitize(q.getQuestionContent())));
            set.getSectionB().forEach(pair -> {
                pair.getChoiceA().setQuestionContent(contentSanitizer.sanitize(pair.getChoiceA().getQuestionContent()));
                pair.getChoiceB().setQuestionContent(contentSanitizer.sanitize(pair.getChoiceB().getQuestionContent()));
            });
        });

        model.addAttribute("subject", subject);
        model.addAttribute("result", result);
        model.addAttribute("examType", examType);
        model.addAttribute("format", format);
        return "generation/review";
    }


    @PostMapping("/{paperId}/swap")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> swapQuestion(@PathVariable Long subjectId,
                                                            @PathVariable Long paperId,
                                                            @RequestParam Long oldQuestionId) {
        try {
            Question newQuestion = generationService.swapQuestion(paperId, oldQuestionId, subjectId);
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
    public ResponseEntity<byte[]> exportDocx(@PathVariable Long subjectId, @PathVariable Long paperId) {
        var paper = generationService.getPaperByIdAndSubjectId(paperId, subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid paper id or subject mismatch"));

        byte[] docxBytes = docxRendererService.exportPaperToDocx(paper);

        String setLabel = (paper.getSetLabel() != null && !paper.getSetLabel().isBlank())
                ? "Set_" + paper.getSetLabel()
                : String.valueOf(paperId);
        String filename = "QuestionPaper_" + setLabel + ".docx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return new ResponseEntity<>(docxBytes, headers, HttpStatus.OK);
    }
}