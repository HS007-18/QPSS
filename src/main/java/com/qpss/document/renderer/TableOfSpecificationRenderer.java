package com.qpss.document.renderer;

import com.qpss.entity.PaperQuestion;
import com.qpss.entity.Question;
import com.qpss.repository.QuestionRepository;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableOfSpecificationRenderer {

    private final QuestionRepository questionRepository;

    public TableOfSpecificationRenderer(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public void renderTableOfSpecification(XWPFDocument document, List<PaperQuestion> sectionA, List<PaperQuestion> sectionB) {
        document.createParagraph().createRun().addBreak();

        boolean hasPartB = sectionB != null && !sectionB.isEmpty();
        int cols = hasPartB ? 5 : 4;
        XWPFTable table = document.createTable(1, cols);
        table.setWidth("100%");
        table.setCellMargins(40, 80, 40, 80);
        
        if (hasPartB) {
            setColumnWidths(table, new int[] { 3000, 1500, 1500, 1500, 2300 });
        } else {
            setColumnWidths(table, new int[] { 3000, 1500, 2000, 2300 });
        }

        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr tblPr = table.getCTTbl().getTblPr();
        if (tblPr == null) tblPr = table.getCTTbl().addNewTblPr();
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTString styleStr = tblPr.isSetTblStyle() ? tblPr.getTblStyle() : tblPr.addNewTblStyle();
        styleStr.setVal("TableGrid");

        int rIdx = 0;
        XWPFTableRow rTitle = getOrCreateRow(table, rIdx++, cols);
        mergeCells(rTitle, 0, cols - 1);
        setCellText(rTitle.getCell(0), "TABLE OF SPECIFICATION", ParagraphAlignment.CENTER, true, 10);

        XWPFTableRow rH1 = getOrCreateRow(table, rIdx++, cols);
        mergeCells(rH1, 0, 1);
        setCellText(rH1.getCell(0), "Revised Bloom\u2019s Taxonomy (RBT)\nCognitive Levels", ParagraphAlignment.CENTER, false, 10);
        
        if (hasPartB) {
            mergeCells(rH1, 2, 3);
            setCellText(rH1.getCell(2), "Marks Distribution", ParagraphAlignment.CENTER, false, 10);
            setCellText(rH1.getCell(4), "Total Marks", ParagraphAlignment.CENTER, false, 10);
        } else {
            setCellText(rH1.getCell(2), "Marks Distribution", ParagraphAlignment.CENTER, false, 10);
            setCellText(rH1.getCell(3), "Total Marks", ParagraphAlignment.CENTER, false, 10);
        }

        XWPFTableRow rH2 = getOrCreateRow(table, rIdx++, cols);
        mergeCells(rH2, 0, 1);
        setCellText(rH2.getCell(0), "", ParagraphAlignment.CENTER, false, 10);
        
        if (hasPartB) {
            setCellText(rH2.getCell(2), "Part - A", ParagraphAlignment.CENTER, false, 10);
            setCellText(rH2.getCell(3), "Part - B", ParagraphAlignment.CENTER, false, 10);
            setCellText(rH2.getCell(4), "", ParagraphAlignment.CENTER, false, 10);
            mergeCellsVertically(table, 0, 1, 2);
            mergeCellsVertically(table, 1, 1, 2);
            mergeCellsVertically(table, 4, 1, 2);
        } else {
            setCellText(rH2.getCell(2), "Part - A", ParagraphAlignment.CENTER, false, 10);
            setCellText(rH2.getCell(3), "", ParagraphAlignment.CENTER, false, 10);
            mergeCellsVertically(table, 0, 1, 2);
            mergeCellsVertically(table, 1, 1, 2);
            mergeCellsVertically(table, 3, 1, 2);
        }

        Map<String, Integer> marksA = calculateRbtMarks(sectionA);
        Map<String, Integer> marksB = hasPartB ? calculateRbtMarks(sectionB) : new HashMap<>();

        String[][] rbtLevels = {
                { "Remember", "R" },
                { "Understand", "U" },
                { "Apply", "Ap" },
                { "Analyze", "Az" },
                { "Evaluate", "E" },
                { "Create", "C" }
        };

        int totalA = 0, totalB = 0;

        for (String[] level : rbtLevels) {
            String name = level[0];
            String code = level[1].toUpperCase();
            int mA = marksA.getOrDefault(code, 0);
            int mB = marksB.getOrDefault(code, 0);
            int total = mA + mB;
            totalA += mA;
            totalB += mB;

            XWPFTableRow rLevel = getOrCreateRow(table, rIdx++, cols);
            setCellText(rLevel.getCell(0), name, ParagraphAlignment.CENTER, false, 10);
            setCellText(rLevel.getCell(1), code, ParagraphAlignment.CENTER, false, 10);
            setCellText(rLevel.getCell(2), mA > 0 ? String.valueOf(mA) : "", ParagraphAlignment.CENTER, false, 10);
            
            if (hasPartB) {
                setCellText(rLevel.getCell(3), mB > 0 ? String.valueOf(mB) : "", ParagraphAlignment.CENTER, false, 10);
                setCellText(rLevel.getCell(4), total > 0 ? String.valueOf(total) : "", ParagraphAlignment.CENTER, false, 10);
            } else {
                setCellText(rLevel.getCell(3), total > 0 ? String.valueOf(total) : "", ParagraphAlignment.CENTER, false, 10);
            }
        }

        XWPFTableRow rTot = getOrCreateRow(table, rIdx++, cols);
        mergeCells(rTot, 0, 1);
        setCellText(rTot.getCell(0), "Total Marks", ParagraphAlignment.CENTER, false, 10);
        setCellText(rTot.getCell(2), String.valueOf(totalA), ParagraphAlignment.CENTER, false, 10);
        
        if (hasPartB) {
            setCellText(rTot.getCell(3), String.valueOf(totalB), ParagraphAlignment.CENTER, false, 10);
            setCellText(rTot.getCell(4), String.valueOf(totalA + totalB), ParagraphAlignment.CENTER, false, 10);
        } else {
            setCellText(rTot.getCell(3), String.valueOf(totalA), ParagraphAlignment.CENTER, false, 10);
        }

        for (XWPFTableRow row : table.getRows()) {
            for (XWPFTableCell cell : row.getTableCells()) {
                cell.setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            }
        }
        applyTableBorders(table);
    }

    private Map<String, Integer> calculateRbtMarks(List<PaperQuestion> pqs) {
        Map<String, Integer> res = new HashMap<>();
        java.util.Set<Integer> processedQuestionNumbers = new java.util.HashSet<>();
        
        for (PaperQuestion pq : pqs) {
            // Only count one choice per question number (e.g., process 11a, ignore 11b)
            if (!processedQuestionNumbers.add(pq.getQuestionNumber())) {
                continue;
            }
            
            Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
            if (q != null && q.getRbt() != null && q.getMarks() != null) {
                String code = q.getRbt().trim().toUpperCase();
                res.put(code, res.getOrDefault(code, 0) + q.getMarks());
            }
        }
        return res;
    }

    // Helper methods
    private XWPFTableRow getOrCreateRow(XWPFTable table, int index, int cols) {
        while (table.getRows().size() <= index) {
            table.createRow();
        }
        XWPFTableRow row = table.getRow(index);
        while (row.getTableCells().size() < cols) {
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

    private void mergeCellsVertically(XWPFTable table, int col, int fromRow, int toRow) {
        for (int rowIndex = fromRow; rowIndex <= toRow; rowIndex++) {
            XWPFTableCell cell = table.getRow(rowIndex).getCell(col);
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr tcPr = cell.getCTTc().isSetTcPr()
                    ? cell.getCTTc().getTcPr()
                    : cell.getCTTc().addNewTcPr();
            if (tcPr.isSetVMerge()) {
                tcPr.getVMerge().setVal(rowIndex == fromRow ? STMerge.RESTART : STMerge.CONTINUE);
            } else {
                tcPr.addNewVMerge().setVal(rowIndex == fromRow ? STMerge.RESTART : STMerge.CONTINUE);
            }
        }
    }

    private void setColumnWidths(XWPFTable table, int[] widths) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl ctTbl = table.getCTTbl();
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid grid = ctTbl.getTblGrid() != null
                ? ctTbl.getTblGrid()
                : ctTbl.addNewTblGrid();
        while (grid.sizeOfGridColArray() > 0) {
            grid.removeGridCol(0);
        }
        for (int w : widths) {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol col = grid.addNewGridCol();
            col.setW(BigInteger.valueOf(w));
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

    private void applyTableBorders(XWPFTable table) {
        table.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
        table.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
        table.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
        table.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
        table.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
        table.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
    }
}