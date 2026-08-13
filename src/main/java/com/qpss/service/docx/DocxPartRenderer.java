package com.qpss.service.docx;

import com.qpss.model.PaperQuestion;
import com.qpss.model.Question;
import com.qpss.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
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

        XWPFTable tableA = document.createTable(sectionA.size() + 1, 5);
        tableA.setWidth("100%");

        XWPFTableRow header = tableA.getRow(0);
        HtmlToWordRenderer.setCellText(header.getCell(0), "Q.No.", true);
        HtmlToWordRenderer.setCellText(header.getCell(1), "Question", true);
        HtmlToWordRenderer.setCellText(header.getCell(2), "M", true);
        HtmlToWordRenderer.setCellText(header.getCell(3), "RBT", true);
        HtmlToWordRenderer.setCellText(header.getCell(4), "CO", true);

        for (int i = 0; i < sectionA.size(); i++) {
            PaperQuestion pq = sectionA.get(i);
            Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
            if (q == null) {
                continue;
            }

            XWPFTableRow row = tableA.getRow(i + 1);
            HtmlToWordRenderer.setCellText(row.getCell(0), String.valueOf(pq.getQuestionNumber()), false);
            HtmlToWordRenderer.setCellHtml(row.getCell(1), q.getQuestionContent());
            HtmlToWordRenderer.setCellText(row.getCell(2), "2", false);
            HtmlToWordRenderer.setCellText(row.getCell(3), q.getRbt(), false);
            HtmlToWordRenderer.setCellText(row.getCell(4), coLabel(q), false);
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

        XWPFTable tableB = document.createTable(sectionB.size() + 1 + (sectionB.size() / 2), 5);
        tableB.setWidth("100%");

        XWPFTableRow header = tableB.getRow(0);
        HtmlToWordRenderer.setCellText(header.getCell(0), "Q.No.", true);
        HtmlToWordRenderer.setCellText(header.getCell(1), "Question", true);
        HtmlToWordRenderer.setCellText(header.getCell(2), "M", true);
        HtmlToWordRenderer.setCellText(header.getCell(3), "RBT", true);
        HtmlToWordRenderer.setCellText(header.getCell(4), "CO", true);

        int rowIndex = 1;
        Integer currentQNum = null;
        for (int i = 0; i < sectionB.size(); i++) {
            PaperQuestion pq = sectionB.get(i);
            Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
            if (q == null) {
                continue;
            }

            if (currentQNum != null && currentQNum.equals(pq.getQuestionNumber())) {
                XWPFTableRow orRow = tableB.getRow(rowIndex++);
                HtmlToWordRenderer.mergeCellsHorizontal(orRow, 0, 4);
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
            HtmlToWordRenderer.setCellHtml(row.getCell(1), q.getQuestionContent());
            HtmlToWordRenderer.setCellText(row.getCell(2), String.valueOf(q.getMarks()), false);
            HtmlToWordRenderer.setCellText(row.getCell(3), q.getRbt(), false);
            HtmlToWordRenderer.setCellText(row.getCell(4), coLabel(q), false);
        }
    }

    private String coLabel(Question q) {
        return q.getCo() != null && q.getCo().toUpperCase().startsWith("CO") ? q.getCo() : "CO" + q.getCo();
    }
}