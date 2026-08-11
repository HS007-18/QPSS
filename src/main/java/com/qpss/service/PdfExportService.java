package com.qpss.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.qpss.model.GeneratedPaper;
import com.qpss.model.PaperQuestion;
import com.qpss.model.Question;
import com.qpss.model.Subject;
import com.qpss.repository.PaperQuestionRepository;
import com.qpss.repository.QuestionRepository;
import com.qpss.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

@Service
@RequiredArgsConstructor
public class PdfExportService {

    private final PaperQuestionRepository paperQuestionRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;

    public byte[] exportPaperToPdf(GeneratedPaper paper) {
        Subject subject = subjectRepository.findById(paper.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        List<PaperQuestion> pqs = paperQuestionRepository.findByPaperIdOrderByQuestionNumberAscChoiceLabelAsc(paper.getId());
        
        List<PaperQuestion> sectionA = pqs.stream()
                .filter(q -> "SECTION_A".equals(q.getSection()))
                .sorted((q1, q2) -> q1.getQuestionNumber().compareTo(q2.getQuestionNumber()))
                .collect(Collectors.toList());

        List<PaperQuestion> sectionB = pqs.stream()
                .filter(q -> "SECTION_B".equals(q.getSection()))
                .sorted((q1, q2) -> {
                    int numCmp = q1.getQuestionNumber().compareTo(q2.getQuestionNumber());
                    if (numCmp != 0) return numCmp;
                    if (q1.getChoiceLabel() == null) return -1;
                    if (q2.getChoiceLabel() == null) return 1;
                    return q1.getChoiceLabel().compareTo(q2.getChoiceLabel());
                })
                .collect(Collectors.toList());

        StringBuilder html = new StringBuilder();
        html.append("<html><head><style>")
            .append("body { font-family: sans-serif; font-size: 14px; margin: 40px; }")
            .append(".text-center { text-align: center; }")
            .append("h1, h2, h3 { margin: 5px 0; }")
            .append(".section-header { text-align: center; font-weight: bold; margin: 20px 0; font-size: 16px; }")
            .append("table { width: 100%; border-collapse: collapse; margin-bottom: 10px; }")
            .append("td { vertical-align: top; padding: 5px; }")
            .append(".q-num { width: 40px; font-weight: bold; }")
            .append(".q-content { width: auto; }")
            .append(".q-meta { width: 60px; text-align: right; color: #555; }")
            .append(".or-divider { text-align: center; font-weight: bold; margin: 10px 0; font-style: italic; }")
            .append("img { max-width: 400px !important; height: auto !important; max-height: 250px !important; display: block; margin: 10px auto; object-fit: contain; }")
            .append("</style></head><body>");

        // Header
        html.append("<div class='text-center'>")
            .append("<h1>COLLEGE OF ENGINEERING</h1>")
            .append("<h2>").append(paper.getExamType().toUpperCase().replace("_", " ")).append(" EXAMINATIONS</h2>")
            .append("<h3>").append(subject.getName()).append("</h3>")
            .append("</div>");

        // Section A
        html.append("<div class='section-header'>PART A - (10 x 2 = 20 marks)</div>");
        html.append("<table>");
        for (PaperQuestion pq : sectionA) {
            Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
            if (q == null) continue;
            html.append("<tr>")
                .append("<td class='q-num'>").append(pq.getQuestionNumber()).append(".</td>")
                .append("<td class='q-content'>").append(q.getQuestionContent()).append("</td>")
                .append("<td class='q-meta'>[CO").append(q.getCo()).append("]</td>")
                .append("</tr>");
        }
        html.append("</table>");

        // Section B
        html.append("<div class='section-header'>PART B - (5 x 16 = 80 marks)</div>");
        
        Integer currentQNum = null;
        for (int i = 0; i < sectionB.size(); i++) {
            PaperQuestion pq = sectionB.get(i);
            Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
            if (q == null) continue;

            if (currentQNum != null && currentQNum.equals(pq.getQuestionNumber())) {
                html.append("<div class='or-divider'>- OR -</div>");
            } else {
                currentQNum = pq.getQuestionNumber();
                if (i > 0) {
                    html.append("<hr style='border: 0; border-top: 1px dashed #ccc; margin: 15px 0;'/>");
                }
            }

            String label = pq.getChoiceLabel() != null ? pq.getChoiceLabel() + ") " : "";
            html.append("<table><tr>")
                .append("<td class='q-num'>").append(pq.getQuestionNumber()).append(". ").append(label).append("</td>")
                .append("<td class='q-content'>").append(q.getQuestionContent()).append("</td>")
                .append("<td class='q-meta'>[CO").append(q.getCo()).append("]</td>")
                .append("</tr></table>");
        }

        html.append("</body></html>");

        String xhtml = html.toString();
        try {
            org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(xhtml);
            jsoupDoc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml);
            xhtml = jsoupDoc.html();
        } catch (Exception e) {
            // fallback
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(xhtml, "");
            builder.toStream(baos);
            builder.run();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }
}
