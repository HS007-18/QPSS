package com.qpss.documentoutput.renderer;
import com.qpss.backend.paper.PaperQuestion;
import com.qpss.backend.questionbank.Question;
import com.qpss.backend.questionbank.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;
import java.util.List;
@Component
@RequiredArgsConstructor
public class DocxPartRenderer {

    private final QuestionRepository questionRepository;

    public void renderPartA(XWPFDocument document, List<PaperQuestion> sectionA) {
        if (sectionA.isEmpty()) {
            return;
        }

        XWPFParagraph partAPara = document.createParagraph();
        partAPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun partARun = partAPara.createRun();
        partARun.setBold(true);
        int qCountA = sectionA.size();
        partARun.setText("PART A - (" + qCountA + " x 2 = " + (qCountA * 2) + " marks)");

        XWPFTable tableA = document.createTable(sectionA.size() + 1, 4);
        tableA.setWidth("100%");
        tableA.setCellMargins(100, 100, 100, 100);

        XWPFTableRow header = tableA.getRow(0);
        HtmlToWordRenderer.setCellText(header.getCell(0), "Q.No.", true);
        HtmlToWordRenderer.setCellText(header.getCell(1), "Question", true);
        HtmlToWordRenderer.setCellText(header.getCell(2), "M", true);
        HtmlToWordRenderer.setCellText(header.getCell(3), "CO", true);

        for (int i = 0; i < sectionA.size(); i++) {
            PaperQuestion pq = sectionA.get(i);
            Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
            if (q == null) {
                continue;
            }

            XWPFTableRow row = tableA.getRow(i + 1);
            HtmlToWordRenderer.setCellText(row.getCell(0), String.valueOf(pq.getQuestionNumber()), false);
            if (q.getStructuredContent() != null && !q.getStructuredContent().isEmpty()) {
                AstToWordRenderer.setCellAst(row.getCell(1), q.getStructuredContent());
            } else {
                HtmlToWordRenderer.setCellHtml(row.getCell(1), q.getQuestionContent());
            }
            HtmlToWordRenderer.setCellText(row.getCell(2), marksLabel(q), false);
            HtmlToWordRenderer.setCellText(row.getCell(3), coLabel(q), false);
        }

        document.createParagraph().createRun().addBreak();
    }

    public void renderPartB(XWPFDocument document, List<PaperQuestion> sectionB) {
        if (sectionB.isEmpty()) {
            return;
        }

        int sampleMarks = 16;
        Question sampleQ = questionRepository.findById(sectionB.get(0).getQuestionId()).orElse(null);
        if (sampleQ != null && sampleQ.getMarks() != null) {
            sampleMarks = sampleQ.getMarks();
        }
        int pairsCount = sectionB.size() / 2;

        XWPFParagraph partBPara = document.createParagraph();
        partBPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun partBRun = partBPara.createRun();
        partBRun.setBold(true);
        partBRun.setText("PART B - (" + pairsCount + " x " + sampleMarks + " = " + (pairsCount * sampleMarks) + " marks)");

        XWPFTable tableB = document.createTable(sectionB.size() + 1 + (sectionB.size() / 2), 4);
        tableB.setWidth("100%");
        tableB.setCellMargins(100, 100, 100, 100);

        XWPFTableRow header = tableB.getRow(0);
        HtmlToWordRenderer.setCellText(header.getCell(0), "Q.No.", true);
        HtmlToWordRenderer.setCellText(header.getCell(1), "Question", true);
        HtmlToWordRenderer.setCellText(header.getCell(2), "M", true);
        HtmlToWordRenderer.setCellText(header.getCell(3), "CO", true);

        int rowIndex = 1;
        Integer currentQNum = null;
        for (int i = 0; i < sectionB.size(); i++) {
            PaperQuestion pq = sectionB.get(i);
            Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);

            if (currentQNum != null && currentQNum.equals(pq.getQuestionNumber())) {
                XWPFTableRow orRow = tableB.getRow(rowIndex++);
                HtmlToWordRenderer.mergeCellsHorizontal(orRow, 0, 3);
                XWPFParagraph orPara = orRow.getCell(0).getParagraphs().get(0);
                orPara.setAlignment(ParagraphAlignment.CENTER);
                XWPFRun orRun = orPara.createRun();
                orRun.setBold(true);
                orRun.setText("(OR)");
            } else {
                currentQNum = pq.getQuestionNumber();
            }

            XWPFTableRow row = tableB.getRow(rowIndex++);
            String label = pq.getChoiceLabel() != null ? pq.getChoiceLabel() + ")" : "";
            HtmlToWordRenderer.setCellText(row.getCell(0), pq.getQuestionNumber() + " " + label, false);
            if (q != null && q.getStructuredContent() != null && !q.getStructuredContent().isEmpty()) {
                AstToWordRenderer.setCellAst(row.getCell(1), q.getStructuredContent());
            } else {
                HtmlToWordRenderer.setCellHtml(row.getCell(1), q != null ? q.getQuestionContent() : "");
            }
            HtmlToWordRenderer.setCellText(row.getCell(2), q != null ? marksLabel(q) : "", false);
            HtmlToWordRenderer.setCellText(row.getCell(3), q != null ? coLabel(q) : "", false);
        }
    }

    private String marksLabel(Question q) {
        if (q.getMarksSplit() != null && !q.getMarksSplit().isBlank()) {
            return q.getMarksSplit();
        }
        return q.getMarks() != null ? String.valueOf(q.getMarks()) : "";
    }

    private String coLabel(Question q) {
        if (q.getCo() == null || q.getCo().isBlank()) {
            return "";
        }
        String co = q.getCo().trim();
        return co.toUpperCase().startsWith("CO") ? co : "CO" + co;
    }
}