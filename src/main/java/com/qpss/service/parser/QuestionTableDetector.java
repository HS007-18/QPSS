package com.qpss.service.parser;

import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.util.List;

public class QuestionTableDetector {

    public boolean isQuestionTable(XWPFTable table) {
        if (table.getRows().isEmpty()) {
            return false;
        }
        
        // Check the first few rows for header keywords, to handle possible merged formatting rows
        int maxRowsToCheck = Math.min(3, table.getRows().size());
        
        for (int i = 0; i < maxRowsToCheck; i++) {
            XWPFTableRow row = table.getRows().get(i);
            List<XWPFTableCell> cells = row.getTableCells();
            
            if (cells.size() >= 5) {
                if (matchesHeader(cells)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    public boolean isHeaderRow(XWPFTableRow row) {
        List<XWPFTableCell> cells = row.getTableCells();
        if (cells.size() >= 5) {
            return matchesHeader(cells);
        }
        return false;
    }

    private boolean matchesHeader(List<XWPFTableCell> cells) {
        String c0 = cleanText(cells.get(0).getText());
        String c1 = cleanText(cells.get(1).getText());
        String c2 = cleanText(cells.get(2).getText());
        String c3 = cleanText(cells.get(3).getText());
        String c4 = cleanText(cells.get(4).getText());

        return (c0.contains("sno") || c0.contains("serial") || c0.contains("qno")) 
                && (c1.contains("question") || c1.contains("desc"))
                && (c2.contains("m") || c2.contains("mark"))
                && (c3.contains("co") || c3.contains("course"))
                && (c4.equals("t") || c4.contains("half"));
    }

    private String cleanText(String text) {
        if (text == null) return "";
        return text.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
