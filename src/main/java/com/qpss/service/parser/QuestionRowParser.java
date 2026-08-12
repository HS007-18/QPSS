package com.qpss.service.parser;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.util.List;

public class QuestionRowParser {

    private final QuestionContentExtractor contentExtractor = new QuestionContentExtractor();

    public ParsedQuestion parseRow(XWPFTableRow row, Integer currentUnit, QuestionParseResult result, XWPFDocument document) {
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

        String questionContent = contentExtractor.extractRichContent(cells.get(1), document);
        String rawOoxml = contentExtractor.extractRawOoxml(cells.get(1));
        
        if (questionContent.isEmpty()) {
            questionContent = null;
        }

        String marksStr = cells.get(2).getText().trim();
        Integer marks = null;
        if (!marksStr.isEmpty()) {
            try {
                marks = Integer.parseInt(marksStr.replaceAll("[^0-9]", ""));
                if (marks != 2 && marks != 16) {
                    marks = null; // invalid marks, treat as missing
                }
            } catch (NumberFormatException e) {
                // invalid marks, treat as missing
            }
        }

        String co = cells.get(3).getText().trim();
        if (co.isEmpty()) {
            co = null;
        }

        String tStr = cells.get(4).getText().trim();
        Integer t = null;
        if (!tStr.isEmpty()) {
            if (tStr.equalsIgnoreCase("I")) {
                t = 1;
            } else if (tStr.equalsIgnoreCase("II")) {
                t = 2;
            } else {
                try {
                    t = Integer.parseInt(tStr.replaceAll("[^0-9]", ""));
                    if (t != 1 && t != 2) {
                        t = null; // invalid T, treat as missing
                    }
                } catch (NumberFormatException e) {
                    // invalid T, treat as missing
                }
            }
        }

        Integer resolvedUnit = currentUnit;
        if (resolvedUnit == null && co != null) {
            try {
                resolvedUnit = Integer.parseInt(co.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                // leave as null
            }
        }

        return ParsedQuestion.builder()
                .serialNo(serialNo)
                .questionContent(questionContent)
                .rawOoxml(rawOoxml)
                .marks(marks)
                .co(co)
                .t(t)
                .unit(resolvedUnit)
                .build();
    }
}
