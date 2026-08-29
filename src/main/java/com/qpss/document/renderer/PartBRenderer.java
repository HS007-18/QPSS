package com.qpss.document.renderer;

import com.qpss.entity.PaperQuestion;
import com.qpss.entity.Question;
import com.qpss.repository.QuestionRepository;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

import java.util.List;

public class PartBRenderer {

    private final QuestionRepository questionRepository;

    public PartBRenderer(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public int renderPartB(XWPFTable table, int rIdx, List<PaperQuestion> sectionB) {
        if (sectionB.isEmpty())
            return rIdx;

        // Dynamically determine Part B marks from actual questions — supports 16-mark and 20-mark
        int partBMarks = 16;
        Question sampleQ = questionRepository.findById(sectionB.get(0).getQuestionId()).orElse(null);
        if (sampleQ != null && sampleQ.getMarks() != null) {
            partBMarks = sampleQ.getMarks();
        }
        int pairsCount = sectionB.size() / 2;

        XWPFTableRow rPartB = getOrCreateRow(table, rIdx++);
        mergeCells(rPartB, 0, 4);
        setCellText(rPartB.getCell(0),
                "PART - B (" + pairsCount + " x " + partBMarks + " = " + (pairsCount * partBMarks) + " Marks)",
                ParagraphAlignment.CENTER, true, 10);

        Integer currentQNum = null;
        for (PaperQuestion pq : sectionB) {
            Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);

            if (currentQNum != null && currentQNum.equals(pq.getQuestionNumber())) {
                XWPFTableRow orRow = getOrCreateRow(table, rIdx++);
                mergeCells(orRow, 0, 4);
                setCellText(orRow.getCell(0), "OR", ParagraphAlignment.CENTER, true, 10);
            } else {
                currentQNum = pq.getQuestionNumber();
            }

            XWPFTableRow r = getOrCreateRow(table, rIdx++);
            setCellText(r.getCell(0),
                    pq.getQuestionNumber() + ".\u00A0(" + (pq.getChoiceLabel() != null ? pq.getChoiceLabel() : "a") + ")",
                    ParagraphAlignment.CENTER, false, 10);
            // Left alignment for questions
            renderQuestionCell(r.getCell(1), q, ParagraphAlignment.LEFT);
            setCellText(r.getCell(2), q != null ? marksLabel(q) : "", ParagraphAlignment.CENTER, false, 10);
            setCellText(r.getCell(3), q != null ? rbtLabel(q) : "", ParagraphAlignment.CENTER, false, 10);
            setCellText(r.getCell(4), q != null ? coLabel(q) : "", ParagraphAlignment.CENTER, false, 10);
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