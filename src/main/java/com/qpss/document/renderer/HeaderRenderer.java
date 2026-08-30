package com.qpss.document.renderer;


import com.qpss.entity.GeneratedPaper;
import com.qpss.entity.Subject;
import com.qpss.document.model.HeaderMetadata;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

public class HeaderRenderer {

    private static final String INSTITUTION_NAME = "KANGEYAM INSTITUTE OF TECHNOLOGY";
    private static final String INSTITUTION_TAGLINE = "(An Autonomous Institution)";

    public int renderHeaderBlock(XWPFTable table, int rIdx, GeneratedPaper paper, Subject subject,
            HeaderMetadata metadata) {
        // Row 0: QP Code and Reg No
        XWPFTableRow r0 = getOrCreateRow(table, rIdx++);
        mergeCells(r0, 0, 4);
        XWPFTableCell c0 = r0.getCell(0);

        CTTbl ctTbl0 = c0.getCTTc().insertNewTbl(0);
        XWPFTable nested0 = new XWPFTable(ctTbl0, c0, 1, 3);
        nested0.setWidth("100%");
        nested0.removeBorders();
        int[] headerWidths = new int[3];
        headerWidths[0] = 3500;
        headerWidths[1] = 4600;
        headerWidths[2] = 1500;
        setColumnWidths(nested0, headerWidths);

        XWPFTableRow nr0 = nested0.getRow(0);
        setCellText(nr0.getCell(0), "QUESTION PAPER CODE:", ParagraphAlignment.LEFT, true, 11);
        setCellText(nr0.getCell(1), "REG. NO.: ...................................", ParagraphAlignment.RIGHT, true, 11);
        setCellText(nr0.getCell(2), " ", ParagraphAlignment.CENTER, false, 11);

        // Row 1: Institution name (constant) + tagline (constant)
        XWPFTableRow r1 = getOrCreateRow(table, rIdx++);
        mergeCells(r1, 0, 4);
        String institution = metadata.getInstitutionName() != null ? metadata.getInstitutionName()
                : INSTITUTION_NAME;
        String tagline = metadata.getTagline() != null ? metadata.getTagline() : INSTITUTION_TAGLINE;
        setCellText(r1.getCell(0), institution + tagline, ParagraphAlignment.CENTER, true, 11);

        // Row 2: Exam title (from metadata or mapped from ExamType)
        XWPFTableRow r2 = getOrCreateRow(table, rIdx++);
        mergeCells(r2, 0, 4);
        String examTitle = resolveExamTitle(paper, metadata);
        String cleanTitle = examTitle.replaceAll("(?i)(B\\.?E\\.?\\s*/\\s*B\\.?Tech\\.?\\s*)", "").trim();
        setCellText(r2.getCell(0), "B.E. / B.Tech. " + cleanTitle, ParagraphAlignment.CENTER, true, 11);

        // Row 3: Academic Year
        XWPFTableRow r3 = getOrCreateRow(table, rIdx++);
        mergeCells(r3, 0, 4);
        setCellText(r3.getCell(0), "Academic Year: ____________", ParagraphAlignment.CENTER, false, 11);

        // Row 4: Date / Session
        XWPFTableRow r4 = getOrCreateRow(table, rIdx++);
        mergeCells(r4, 0, 4);
        setCellText(r4.getCell(0), "Date / Session: ____________", ParagraphAlignment.CENTER, false, 11);

        // Row 5: Semester (Regulation)
        XWPFTableRow r5 = getOrCreateRow(table, rIdx++);
        mergeCells(r5, 0, 4);
        String semStr = metadata.getSemester() != null ? metadata.getSemester() : "";
        String regStr = metadata.getRegulation() != null ? metadata.getRegulation() : "";
        String semReg = semStr;
        if (!regStr.isEmpty()) {
            semReg += " (" + regStr + ")";
        }
        setCellText(r5.getCell(0), semReg, ParagraphAlignment.CENTER, true, 11);

        // Row 6: Department
        XWPFTableRow r6 = getOrCreateRow(table, rIdx++);
        mergeCells(r6, 0, 4);
        String deptStr = metadata.getDepartment() != null ? metadata.getDepartment() : "";
        setCellText(r6.getCell(0), deptStr, ParagraphAlignment.CENTER, true, 11);

        // Row 7: Subject code and title
        XWPFTableRow r7 = getOrCreateRow(table, rIdx++);
        mergeCells(r7, 0, 4);
        String subjCodeTitle = metadata.getSubjectCodeTitle() != null ? metadata.getSubjectCodeTitle()
                : (subject.getName() != null ? subject.getName().toUpperCase() : "");
        setCellText(r7.getCell(0), subjCodeTitle, ParagraphAlignment.CENTER, true, 11);

        // Row 8: Common-to (from source, skip if absent)
        if (metadata.getCommonTo() != null && !metadata.getCommonTo().isBlank()) {
            XWPFTableRow rCommon = getOrCreateRow(table, rIdx++);
            mergeCells(rCommon, 0, 4);
            setCellText(rCommon.getCell(0), metadata.getCommonTo(), ParagraphAlignment.CENTER, false, 11);
        }

        // Row 9: Note (from source, skip if absent)
        if (metadata.getNotes() != null && !metadata.getNotes().isBlank()) {
            XWPFTableRow rNote = getOrCreateRow(table, rIdx++);
            mergeCells(rNote, 0, 4);
            setCellText(rNote.getCell(0), metadata.getNotes(), ParagraphAlignment.CENTER, false, 11);
        }

        return rIdx;
    }

    private String resolveExamTitle(GeneratedPaper paper, HeaderMetadata metadata) {
        // Use source document exam title if available
        if (metadata.getExamTitle() != null && !metadata.getExamTitle().isBlank()) {
            return metadata.getExamTitle();
        }
        // Map from exam type enum
        if (paper.getExamType() != null) {
            String type = paper.getExamType().toUpperCase();
            if (type.equals("INTERNAL_1")) {
                return "CONTINUOUS INTERNAL ASSESSMENT EXAMINATIONS - I";
            } else if (type.equals("INTERNAL_2")) {
                return "CONTINUOUS INTERNAL ASSESSMENT EXAMINATIONS - II";
            } else if (type.equals("SEMESTER")) {
                String session = metadata.getExamSession();
                if (session != null && !session.isBlank()) {
                    return "END SEMESTER EXAMINATIONS, " + session;
                }
                return "END SEMESTER EXAMINATIONS";
            }
        }
        return "EXAMINATIONS";
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

    private void setColumnWidths(XWPFTable table, int[] widths) {
        CTTbl ctTbl = table.getCTTbl();
        CTTblGrid grid = ctTbl.getTblGrid() != null
                ? ctTbl.getTblGrid()
                : ctTbl.addNewTblGrid();
        while (grid.sizeOfGridColArray() > 0) {
            grid.removeGridCol(0);
        }
        for (int w : widths) {
            CTTblGridCol col = grid.addNewGridCol();
            col.setW(java.math.BigInteger.valueOf(w));
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