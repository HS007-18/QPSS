package com.qpss.document.renderer;

import com.qpss.entity.GeneratedPaper;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

public class SignaturesRenderer {

    public void renderSignaturesBlock(XWPFDocument document, GeneratedPaper paper) {
        document.createParagraph().createRun().addBreak();

        XWPFTable table = document.createTable(1, 3);
        table.setWidth("100%");
        table.setCellMargins(0, 0, 0, 0);
        table.removeBorders();

        int rIdx = 0;
        boolean isEse = paper.getExamType() != null
                && (paper.getExamType().equalsIgnoreCase("ESE") || paper.getExamType().equalsIgnoreCase("FAT"));

        if (isEse) {
            XWPFTableRow r1 = getOrCreateRowSignatures(table, rIdx++);
            setCellText(r1.getCell(2), "Audited by", ParagraphAlignment.CENTER, false, 11);

            XWPFTableRow rSpace = getOrCreateRowSignatures(table, rIdx++);
            setCellText(rSpace.getCell(0), "\n\n", ParagraphAlignment.CENTER, false, 11);

            XWPFTableRow r2 = getOrCreateRowSignatures(table, rIdx++);
            setCellText(r2.getCell(2), "\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\nScrutiny Member", ParagraphAlignment.CENTER, false, 11);
        } else {
            XWPFTableRow r1 = getOrCreateRowSignatures(table, rIdx++);
            setCellText(r1.getCell(0), "Verified by", ParagraphAlignment.CENTER, false, 11);
            setCellText(r1.getCell(1), "Approved by", ParagraphAlignment.CENTER, false, 11);
            setCellText(r1.getCell(2), "Audited by", ParagraphAlignment.CENTER, false, 11);

            XWPFTableRow rSpace = getOrCreateRowSignatures(table, rIdx++);
            setCellText(rSpace.getCell(0), "\n\n", ParagraphAlignment.CENTER, false, 11);

            XWPFTableRow r2 = getOrCreateRowSignatures(table, rIdx++);
            setCellText(r2.getCell(0), "\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\nCourse Instructor /\nCoordinator", ParagraphAlignment.CENTER, false, 11);
            setCellText(r2.getCell(1), "\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\nHead of the\nDepartment", ParagraphAlignment.CENTER, false, 11);
            setCellText(r2.getCell(2), "\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\u2026\nIQAC\nCoordinator", ParagraphAlignment.CENTER, false, 11);
        }

        XWPFParagraph endPara = document.createParagraph();
        endPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun endRun = endPara.createRun();
        endRun.setText("***");
        endRun.setFontFamily("Times New Roman");
        endRun.setFontSize(11);
    }

    private XWPFTableRow getOrCreateRowSignatures(XWPFTable table, int index) {
        while (table.getRows().size() <= index) {
            table.createRow();
        }
        XWPFTableRow row = table.getRow(index);
        while (row.getTableCells().size() < 3) {
            row.createCell();
        }
        return row;
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