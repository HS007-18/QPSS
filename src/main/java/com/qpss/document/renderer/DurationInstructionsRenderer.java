package com.qpss.document.renderer;

import com.qpss.entity.GeneratedPaper;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;

public class DurationInstructionsRenderer {

    private static final int MAXIMUM_MARKS = 100;

    public int renderDurationAndInstructionsBlock(XWPFTable table, int rIdx, GeneratedPaper paper) {
        String durationStr = paper.getDuration() != null ? paper.getDuration() : "Three Hours";
        XWPFTableRow rDur = getOrCreateRow(table, rIdx++);
        mergeCells(rDur, 0, 4);
        XWPFTableCell c0 = rDur.getCell(0);

        CTTbl ctTbl = c0.getCTTc().insertNewTbl(0);
        XWPFTable nested = new XWPFTable(ctTbl, c0, 1, 2);
        nested.setWidth("100%");
        nested.removeBorders();
        XWPFTableRow nr = nested.getRow(0);

        setCellText(nr.getCell(0), "Duration: " + durationStr, ParagraphAlignment.LEFT, true, 10);
        setCellText(nr.getCell(1), "Maximum Marks:\u00A0" + MAXIMUM_MARKS, ParagraphAlignment.RIGHT, true, 10);

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