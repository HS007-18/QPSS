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
@Deprecated // Use DocxMasterTableRenderer instead
public class DocxPartRenderer {

    private final QuestionRepository questionRepository;

    @Deprecated
    public void renderPartA(XWPFDocument document, List<PaperQuestion> sectionA) {
        if (sectionA.isEmpty()) {
            return;
        }

        XWPFParagraph partAPara = document.createParagraph();
        partAPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun partARun = partAPara.createRun();
        partARun.setBold(true);
        int qCountA = sectionA.size();
        int perQuestionMarks = 2;
        Question firstQ = questionRepository.findById(sectionA.get(0).getQuestionId()).orElse(null);
        if (firstQ != null && firstQ.getMarks() != null) {
            perQuestionMarks = firstQ.getMarks();
        }
        partARun.setText("PART A - (" + qCountA + " x " + perQuestionMarks + " = " + (qCountA * perQuestionMarks) + " marks)");

        XWPFTable tableA = document.createTable(sectionA.size() + 1, 5);
        tableA.setWidth("100%");
        tableA.setCellMargins(100, 100, 100, 100);

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
            if (q.getStructuredContent() != null && !q.getStructuredContent().isEmpty()) {
                AstToWordRenderer.setCellAst(row.getCell(1), q.getStructuredContent());
            } else {
                HtmlToWordRenderer.setCellHtml(row.getCell(1), q.getQuestionContent());
            }
            HtmlToWordRenderer.setCellText(row.getCell(2), marksLabel(q), false);
            HtmlToWordRenderer.setCellText(row.getCell(3), rbtLabel(q), false);
            HtmlToWordRenderer.setCellText(row.getCell(4), coLabel(q), false);
        }

        applyTableStyling(tableA);

        document.createParagraph().createRun().addBreak();
    }

    @Deprecated
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
        partBRun.setText(
                "PART B - (" + pairsCount + " x " + sampleMarks + " = " + (pairsCount * sampleMarks) + " marks)");

        XWPFTable tableB = document.createTable(sectionB.size() + 1 + (sectionB.size() / 2), 5);
        tableB.setWidth("100%");
        tableB.setCellMargins(100, 100, 100, 100);

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
            if (q != null && q.getStructuredContent() != null && !q.getStructuredContent().isEmpty()) {
                AstToWordRenderer.setCellAst(row.getCell(1), q.getStructuredContent());
            } else {
                HtmlToWordRenderer.setCellHtml(row.getCell(1), q != null ? q.getQuestionContent() : "");
            }
            HtmlToWordRenderer.setCellText(row.getCell(2), q != null ? marksLabel(q) : "", false);
            HtmlToWordRenderer.setCellText(row.getCell(3), q != null ? rbtLabel(q) : "", false);
            HtmlToWordRenderer.setCellText(row.getCell(4), q != null ? coLabel(q) : "", false);
        }

        applyTableStyling(tableB);
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

    private String rbtLabel(Question q) {
        if (q.getRbt() == null || q.getRbt().isBlank()) {
            return "";
        }
        return q.getRbt().trim().toUpperCase();
    }

    private void applyTableStyling(XWPFTable table) {
        table.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
        table.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
        table.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
        table.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
        table.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
        table.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");

        for (XWPFTableRow row : table.getRows()) {
            for (org.apache.poi.xwpf.usermodel.XWPFTableCell cell : row.getTableCells()) {
                for (XWPFParagraph p : cell.getParagraphs()) {
                    p.setAlignment(ParagraphAlignment.BOTH);
                }
            }
        }
    }
}