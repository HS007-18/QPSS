package com.qpss.document.parser;

import com.qpss.document.model.ParsedQuestion;
import com.qpss.document.model.QuestionConstants;
import com.qpss.document.model.QuestionParseResult;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import java.util.List;
import static com.qpss.document.parser.CellTextReader.cell;
import static com.qpss.document.parser.CellTextReader.cellText;
import static com.qpss.document.parser.CellTextReader.isBlank;
public class QuestionRowParser {

    private final QuestionContentExtractor contentExtractor = new QuestionContentExtractor();

    public ParsedQuestion parseRow(XWPFTableRow row, ColumnLayout layout, Integer currentUnit,
                                   QuestionParseResult result, XWPFDocument document) {
        if (!layout.isValid()) {
            return null;
        }

        List<XWPFTableCell> cells = row.getTableCells();

        String snoStr = cellText(cells, layout.indexOf(ColumnLayout.Role.SNO));
        Integer serialNo = 0;
        if (!isBlank(snoStr)) {
            try {
                serialNo = Integer.parseInt(snoStr.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                serialNo = 0;
            }
        }

        com.qpss.document.parser.ContentExtractionResult contentResult = contentExtractor.extractStructuredContent(
                cell(cells, layout.indexOf(ColumnLayout.Role.QUESTION)), document);
        
        String questionContent = contentResult.getHtmlFallback();
        String structuredContent = null;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            structuredContent = mapper.writerFor(new com.fasterxml.jackson.core.type.TypeReference<List<com.qpss.document.model.ast.AstNode>>() {})
                                      .writeValueAsString(contentResult.getAstNodes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        String marksRaw = cellText(cells, layout.indexOf(ColumnLayout.Role.MARKS));
        Integer marks = parseMarks(marksRaw);
        String marksSplit = parseMarksSplit(marksRaw, marks);
        String rbt = normalizeRbt(cellText(cells, layout.indexOf(ColumnLayout.Role.RBT)));
        String co = cellText(cells, layout.indexOf(ColumnLayout.Role.CO));
        Integer[] tAndTopic = parseTAndTopic(cellText(cells, layout.indexOf(ColumnLayout.Role.T)));
        Integer t = tAndTopic[0];
        Integer topic = tAndTopic[1];
        String questionType = normalizeType(cellText(cells, layout.indexOf(ColumnLayout.Role.TYPE)));

        Integer resolvedUnit = currentUnit;
        if (resolvedUnit == null && !isBlank(co)) {
            try {
                int fromCo = Integer.parseInt(co.replaceAll("[^0-9]", ""));
                if (fromCo >= 1 && fromCo <= 5) {
                    resolvedUnit = fromCo;
                }
            } catch (NumberFormatException e) {
                resolvedUnit = null;
            }
        }

        ParsedQuestion pq = ParsedQuestion.builder()
                .serialNo(serialNo)
                .questionContent(isBlank(questionContent) ? null : questionContent)
                .structuredContent(structuredContent)
                .marks(marks)
                .marksSplit(marksSplit)
                .rbt(rbt)
                .co(isBlank(co) ? null : co)
                .t(t)
                .topic(topic)
                .unit(resolvedUnit)
                .questionType(questionType)
                .build();

        if (!pq.isComplete()) {
            result.addError("Question S.No " + serialNo + " is incomplete, missing fields: " + pq.missingFields());
            return null;
        }

        return pq;
    }

    private String parseMarksSplit(String raw, Integer marks) {
        if (isBlank(raw) || !raw.contains("+") || marks == null) {
            return null;
        }
        String normalized = raw.replaceAll("\\s+", "");
        if (normalized.matches("\\d+\\+\\d+")) {
            int total = 0;
            for (String part : normalized.split("\\+")) {
                total += Integer.parseInt(part);
            }
            if (total == marks) {
                return normalized;
            }
        }
        return null;
    }

    private Integer parseMarks(String raw) {
        if (isBlank(raw)) {
            return null;
        }
        try {
            if (raw.contains("+")) {
                int total = 0;
                for (String part : raw.split("\\+")) {
                    total += Integer.parseInt(part.replaceAll("[^0-9]", ""));
                }
                return QuestionConstants.MARK_VALUES.contains(total) ? total : null;
            }
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

    private Integer[] parseTAndTopic(String raw) {
        if (isBlank(raw)) {
            return new Integer[]{null, null};
        }
        String value = raw.trim().toUpperCase();
        if (value.equals("I")) return new Integer[]{1, 1};
        if (value.equals("II")) return new Integer[]{2, 2};
        if (value.equals("III")) return new Integer[]{1, 3};
        if (value.equals("IV")) return new Integer[]{1, 4};
        if (value.equals("V")) return new Integer[]{1, 5};
        if (value.equals("VI")) return new Integer[]{1, 6};
        if (value.equals("VII")) return new Integer[]{1, 7};
        if (value.equals("VIII")) return new Integer[]{1, 8};
        if (value.equals("IX")) return new Integer[]{1, 9};
        if (value.equals("X")) return new Integer[]{1, 10};
        if (value.equals("XI")) return new Integer[]{1, 11};
        if (value.equals("XII")) return new Integer[]{1, 12};
        if (value.equals("XIII")) return new Integer[]{1, 13};
        if (value.equals("XIV")) return new Integer[]{1, 14};
        if (value.equals("XV")) return new Integer[]{1, 15};
        if (value.equals("XVI")) return new Integer[]{1, 16};
        if (value.equals("XVII")) return new Integer[]{1, 17};
        if (value.equals("XVIII")) return new Integer[]{1, 18};
        if (value.equals("XIX")) return new Integer[]{1, 19};
        if (value.equals("XX")) return new Integer[]{1, 20};

        try {
            Integer numericVal = Integer.parseInt(value.replaceAll("[^0-9]", ""));
            if (QuestionConstants.T_VALUES.contains(numericVal)) {
                return new Integer[]{numericVal, numericVal};
            } else {
                return new Integer[]{1, numericVal};
            }
        } catch (NumberFormatException e) {
            return new Integer[]{null, null};
        }
    }

    private String normalizeType(String raw) {
        if (isBlank(raw)) {
            return null;
        }
        String type = raw.trim();
        if (type.length() > 10) {
            type = type.substring(0, 10);
        }
        return type;
    }
}