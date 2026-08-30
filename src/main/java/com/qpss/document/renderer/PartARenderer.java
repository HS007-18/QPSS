package com.qpss.document.renderer;

import com.qpss.entity.PaperQuestion;
import com.qpss.entity.Question;
import com.qpss.repository.QuestionRepository;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

import java.util.List;

public class PartARenderer {

    private final QuestionRepository questionRepository;

    public PartARenderer(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public int renderPartA(XWPFTable table, int rIdx, List<PaperQuestion> sectionA) {
        if (sectionA.isEmpty())
            return rIdx;

        // Dynamically determine Part A marks from actual questions
        int qCount = sectionA.size();
        int perQuestionMarks = 2; // default
        Question firstA = questionRepository.findById(sectionA.get(0).getQuestionId()).orElse(null);
        if (firstA != null && firstA.getMarks() != null) {
            perQuestionMarks = firstA.getMarks();
        }

        XWPFTableRow rPartA = getOrCreateRow(table, rIdx++);
        mergeCells(rPartA, 0, 4);
        setCellText(rPartA.getCell(0), "PART - A (" + qCount + " x " + perQuestionMarks + " = " + (qCount * perQuestionMarks) + " Marks)",
                ParagraphAlignment.CENTER, true, 10);

        XWPFTableRow rInst = getOrCreateRow(table, rIdx++);
        mergeCells(rInst, 0, 4);
        setCellText(rInst.getCell(0), "Answer ALL Questions.", ParagraphAlignment.CENTER, true, 10);

        XWPFTableRow rHeader = getOrCreateRow(table, rIdx++);
        setCellText(rHeader.getCell(0), "", ParagraphAlignment.CENTER, true, 10);
        setCellText(rHeader.getCell(1), "", ParagraphAlignment.CENTER, true, 10);
        setCellText(rHeader.getCell(2), "M", ParagraphAlignment.CENTER, true, 10);
        setCellText(rHeader.getCell(3), "RBT", ParagraphAlignment.CENTER, true, 10);
        setCellText(rHeader.getCell(4), "CO", ParagraphAlignment.CENTER, true, 10);

        for (PaperQuestion pq : sectionA) {
            Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
            if (q == null)
                continue;

            XWPFTableRow r = getOrCreateRow(table, rIdx++);
            setCellText(r.getCell(0), pq.getQuestionNumber() + ".", ParagraphAlignment.LEFT, false, 10);
            // Left alignment for questions
            renderQuestionCell(r.getCell(1), q, ParagraphAlignment.LEFT);
            setCellText(r.getCell(2), marksLabel(q), ParagraphAlignment.CENTER, false, 10);
            setCellText(r.getCell(3), rbtLabel(q), ParagraphAlignment.CENTER, false, 10);
            setCellText(r.getCell(4), coLabel(q), ParagraphAlignment.CENTER, false, 10);
        }

        return rIdx;
    }

    private void renderQuestionCell(XWPFTableCell cell, Question q, ParagraphAlignment alignment) {
        if (q != null && q.getStructuredContent() != null && !q.getStructuredContent().isEmpty()) {
            AstToWordRenderer.setCellAst(cell, q.getStructuredContent());
        } else {
            HtmlToWordRenderer.setCellHtml(cell, q != null ? q.getQuestionContent() : "");
        }

        // Enforce alignment on the cell's paragraphs
        for (XWPFParagraph p : cell.getParagraphs()) {
            p.setAlignment(alignment);
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

    private String rbtLabel(Question q) {
        if (q.getRbt() == null || q.getRbt().isBlank()) {
            return "";
        }
        return q.getRbt().trim().toUpperCase();
    }

    // Helper methods
    private XWPFTableRow getOrCreateRow(XWPFTable table, int index) {
        while (table.getRows().size() <= index) {
            table.createRow();
        }
        XWPFTableRow row = table.getRow(index);
        while (row.getTableCells().size() < 5) {
            row.createCell();
        }
        return row;
    }

    private void mergeCells(XWPFTableRow row, int fromCol, int toCol) {
        for (int cellIndex = fromCol; cellIndex <= toCol; cellIndex++) {
            XWPFTableCell cell = row.getCell(cellIndex);
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr tcPr = cell.getCTTc().isSetTcPr()
                    ? cell.getCTTc().getTcPr()
                    : cell.getCTTc().addNewTcPr();
            if (tcPr.isSetHMerge()) {
                tcPr.getHMerge().setVal(cellIndex == fromCol ? STMerge.RESTART : STMerge.CONTINUE);
            } else {
                tcPr.addNewHMerge().setVal(cellIndex == fromCol ? STMerge.RESTART : STMerge.CONTINUE);
            }
        }
    }

    private void setCellText(XWPFTableCell cell, String text, ParagraphAlignment align, boolean bold, int fontSize) {
        if (cell.getParagraphs().isEmpty()) {
            cell.addParagraph();
        }
        XWPFParagraph p = cell.getParagraphs().get(0);
        p.setAlignment(align);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        p.setSpacingBetween(1.0, LineSpacingRule.AUTO);

        // Remove existing runs
        while (p.getRuns().size() > 0) {
            p.removeRun(0);
        }

        if (text != null && !text.isEmpty()) {
            String[] lines = text.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String[] parts = lines[i].split("\t", -1);
                for (int j = 0; j < parts.length; j++) {
                    XWPFRun r = p.createRun();
                    r.setText(parts[j]);
                    r.setBold(bold);
                    r.setFontFamily("Times New Roman");
                    r.setFontSize(fontSize);
                    if (j < parts.length - 1) {
                        r.addTab();
                    }
                }
                if (i < lines.length - 1) {
                    p.createRun().addBreak();
                }
            }
        }
    }
}