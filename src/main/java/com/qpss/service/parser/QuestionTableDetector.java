package com.qpss.service.parser;

import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.util.List;

public class QuestionTableDetector {

    public XWPFTableRow findHeaderRow(XWPFTable table) {
        if (table.getRows().isEmpty()) {
            return null;
        }
        int maxRowsToCheck = Math.min(3, table.getRows().size());
        for (int i = 0; i < maxRowsToCheck; i++) {
            XWPFTableRow row = table.getRows().get(i);
            if (isHeaderRow(row)) {
                return row;
            }
        }
        return null;
    }

    public boolean isHeaderRow(XWPFTableRow row) {
        List<XWPFTableCell> cells = row.getTableCells();
        return cells.size() >= 3 && matchesHeader(cells);
    }

    private boolean matchesHeader(List<XWPFTableCell> cells) {
        String c0 = cleanText(cells.get(0).getText());
        String c1 = cleanText(cells.get(1).getText());
        String c2 = cleanText(cells.get(2).getText());

        boolean hasSno = c0.contains("sno") || c0.contains("serial") || c0.contains("qno")
                || c0.equals("no") || c0.endsWith("no");
        boolean hasQuestion = c1.contains("question") || c1.contains("desc") || c1.contains("part");
        boolean hasMarks = c2.contains("m") || c2.contains("mark");

        return hasSno && hasQuestion && hasMarks;
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}