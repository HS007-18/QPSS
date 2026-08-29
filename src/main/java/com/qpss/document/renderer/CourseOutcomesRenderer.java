package com.qpss.document.renderer;

import com.qpss.entity.GeneratedPaper;
import com.qpss.document.model.HeaderMetadata;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

public class CourseOutcomesRenderer {

    public int renderCourseOutcomesBlock(XWPFTable table, int rIdx, GeneratedPaper paper, HeaderMetadata metadata) {
        // Render ALL COs from source document without hardcoded filtering.
        // The source document already contains the applicable COs for this exam.
        java.util.List<HeaderMetadata.CourseOutcome> cos = metadata.getCourseOutcomes() != null ? metadata.getCourseOutcomes()
                : java.util.List.of();

        if (cos.isEmpty()) {
            return rIdx; // No COs in source — skip entirely, do not fabricate
        }

        XWPFTableRow rHead = getOrCreateRow(table, rIdx++);
        mergeCells(rHead, 0, 4);
        setCellText(rHead.getCell(0), "COURSE OUTCOMES (COs): Students will be able to", ParagraphAlignment.LEFT, true,
                10);

        for (HeaderMetadata.CourseOutcome co : cos) {
            XWPFTableRow rCo = getOrCreateRow(table, rIdx++);
            mergeCells(rCo, 1, 4);
            setCellText(rCo.getCell(0), co.getCode() + ":", ParagraphAlignment.LEFT, true, 10);
            setCellText(rCo.getCell(1), co.getDescription(), ParagraphAlignment.LEFT, false, 10);
        }

        return rIdx;
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