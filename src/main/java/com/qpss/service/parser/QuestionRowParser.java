package com.qpss.service.parser;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.util.List;

import static com.qpss.service.parser.CellTextReader.cell;
import static com.qpss.service.parser.CellTextReader.cellText;
import static com.qpss.service.parser.CellTextReader.isBlank;

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
        String marksRaw = cellText(cells, layout.indexOf(ColumnLayout.Role.MARKS));
        Integer marks = parseMarks(marksRaw);
        String marksSplit = parseMarksSplit(marksRaw, marks);
        String rbt = normalizeRbt(cellText(cells, layout.indexOf(ColumnLayout.Role.RBT)));
        String co = cellText(cells, layout.indexOf(ColumnLayout.Role.CO));
        Integer t = parseT(cellText(cells, layout.indexOf(ColumnLayout.Role.T)));
        String questionType = normalizeType(cellText(cells, layout.indexOf(ColumnLayout.Role.TYPE)));

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
                .marksSplit(marksSplit)
                .rbt(rbt)
                .co(isBlank(co) ? null : co)
                .t(t)
                .unit(resolvedUnit)
                .questionType(questionType)
                .build();
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