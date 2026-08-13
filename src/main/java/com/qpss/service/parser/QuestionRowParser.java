package com.qpss.service.parser;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.util.List;

public class QuestionRowParser {

    private final QuestionContentExtractor contentExtractor = new QuestionContentExtractor();

    public ParsedQuestion parseRow(XWPFTableRow row, ColumnLayout layout, Integer currentUnit,
                                   QuestionParseResult result, XWPFDocument document) {
        if (!layout.isValid()) {
            return null;
        }

        List<XWPFTableCell> cells = row.getTableCells();

        String snoStr = cellText(cells, layout.indexOf(ColumnLayout.Role.SNO));
        if (isBlank(snoStr)) {
            return null;
        }

        Integer serialNo;
        try {
            serialNo = Integer.parseInt(snoStr.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }

        String questionContent = contentExtractor.extractRichContent(
                cell(cells, layout.indexOf(ColumnLayout.Role.QUESTION)), document);
        Integer marks = parseMarks(cellText(cells, layout.indexOf(ColumnLayout.Role.MARKS)));
        String rbt = normalizeRbt(cellText(cells, layout.indexOf(ColumnLayout.Role.RBT)));
        String co = cellText(cells, layout.indexOf(ColumnLayout.Role.CO));
        Integer t = parseT(cellText(cells, layout.indexOf(ColumnLayout.Role.T)));

        Integer resolvedUnit = currentUnit;
        if (resolvedUnit == null && !isBlank(co)) {
            try {
                resolvedUnit = Integer.parseInt(co.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                resolvedUnit = null;
            }
        }

        return ParsedQuestion.builder()
                .serialNo(serialNo)
                .questionContent(isBlank(questionContent) ? null : questionContent)
                .marks(marks)
                .rbt(rbt)
                .co(isBlank(co) ? null : co)
                .t(t)
                .unit(resolvedUnit)
                .build();
    }

    private Integer parseMarks(String raw) {
        if (isBlank(raw)) {
            return null;
        }
        try {
            Integer marks = Integer.parseInt(raw.replaceAll("[^0-9]", ""));
            return QuestionConstants.MARK_VALUES.contains(marks) ? marks : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String normalizeRbt(String raw) {
        if (isBlank(raw)) {
            return null;
        }
        String rbt = raw.trim().toUpperCase();
        return QuestionConstants.RBT_VALUES.contains(rbt) ? rbt : null;
    }

    private Integer parseT(String raw) {
        if (isBlank(raw)) {
            return null;
        }
        String value = raw.trim();
        if (value.equalsIgnoreCase("I")) {
            return 1;
        }
        if (value.equalsIgnoreCase("II")) {
            return 2;
        }
        try {
            Integer t = Integer.parseInt(value.replaceAll("[^0-9]", ""));
            return QuestionConstants.T_VALUES.contains(t) ? t : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private XWPFTableCell cell(List<XWPFTableCell> cells, int index) {
        if (index < 0 || index >= cells.size()) {
            return null;
        }
        return cells.get(index);
    }

    private String cellText(List<XWPFTableCell> cells, int index) {
        XWPFTableCell cell = cell(cells, index);
        if (cell == null) {
            return "";
        }
        String text = cell.getText();
        return text == null ? "" : text.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}