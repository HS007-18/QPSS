package com.qpss.service.parser;

import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.util.List;

public class QuestionRowParser {

    private final QuestionContentExtractor contentExtractor = new QuestionContentExtractor();

    public ParsedQuestion parseRow(XWPFTableRow row, Integer currentUnit, QuestionParseResult result) {
        List<XWPFTableCell> cells = row.getTableCells();
        if (cells.size() < 5) {
            return null;
        }

        String snoStr = cells.get(0).getText().trim();
        if (snoStr.isEmpty()) {
            return null; 
        }

        Integer serialNo;
        try {
            serialNo = Integer.parseInt(snoStr.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null; // Not a valid question row
        }

        String questionContent = contentExtractor.extractPlainText(cells.get(1));
        String rawOoxml = contentExtractor.extractRawOoxml(cells.get(1));
        
        if (questionContent.isEmpty()) {
            result.addError("Row " + snoStr + ": Missing question content.");
            return null;
        }

        String marksStr = cells.get(2).getText().trim();
        Integer marks;
        try {
            marks = Integer.parseInt(marksStr.replaceAll("[^0-9]", ""));
            if (marks != 2 && marks != 16) {
                result.addError("Row " + snoStr + ": Invalid marks (" + marksStr + "). Only 2 or 16 supported.");
                return null;
            }
        } catch (NumberFormatException e) {
            result.addError("Row " + snoStr + ": Invalid marks format (" + marksStr + ").");
            return null;
        }

        String co = cells.get(3).getText().trim();
        if (co.isEmpty()) {
            result.addError("Row " + snoStr + ": Missing CO.");
            return null;
        }

        String tStr = cells.get(4).getText().trim();
        Integer t;
        try {
            t = Integer.parseInt(tStr.replaceAll("[^0-9]", ""));
            if (t != 1 && t != 2) {
                result.addError("Row " + snoStr + ": Invalid T (" + tStr + "). Only 1 or 2 supported.");
                return null;
            }
        } catch (NumberFormatException e) {
            result.addError("Row " + snoStr + ": Invalid T format (" + tStr + ").");
            return null;
        }

        if (currentUnit == null) {
            result.addError("Row " + snoStr + ": Unit not identified from context. Cannot proceed.");
            return null;
        }

        return ParsedQuestion.builder()
                .serialNo(serialNo)
                .questionContent(questionContent)
                .rawOoxml(rawOoxml)
                .marks(marks)
                .co(co)
                .t(t)
                .unit(currentUnit)
                .build();
    }
}
