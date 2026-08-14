package com.qpss.service.parser;

import org.apache.poi.xwpf.usermodel.XWPFTableCell;

import java.util.List;

public final class CellTextReader {

    private CellTextReader() {
    }

    public static XWPFTableCell cell(List<XWPFTableCell> cells, int index) {
        if (index < 0 || index >= cells.size()) {
            return null;
        }
        return cells.get(index);
    }

    public static String cellText(List<XWPFTableCell> cells, int index) {
        XWPFTableCell cell = cell(cells, index);
        if (cell == null) {
            return "";
        }
        String text = cell.getText();
        return text == null ? "" : text.trim();
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
