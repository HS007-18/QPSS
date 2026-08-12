package com.qpss.service.parser;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.InputStream;
import java.util.List;

public class DocxDocumentReader {

    private final QuestionTableDetector tableDetector = new QuestionTableDetector();
    private final QuestionRowParser rowParser = new QuestionRowParser();

    public QuestionParseResult parse(InputStream inputStream, String filename) {
        QuestionParseResult result = new QuestionParseResult();
        UnitContextResolver unitResolver = new UnitContextResolver(filename);

        try (XWPFDocument doc = new XWPFDocument(inputStream)) {
            List<IBodyElement> bodyElements = doc.getBodyElements();
            for (IBodyElement element : bodyElements) {
                if (element instanceof XWPFParagraph) {
                    XWPFParagraph p = (XWPFParagraph) element;
                    unitResolver.processParagraph(p.getText());
                } else if (element instanceof XWPFTable) {
                    XWPFTable table = (XWPFTable) element;
                    if (tableDetector.isQuestionTable(table)) {
                        for (XWPFTableRow row : table.getRows()) {
                            if (tableDetector.isHeaderRow(row)) {
                                continue;
                            }
                            ParsedQuestion q = rowParser.parseRow(row, unitResolver.getCurrentUnit(), result, doc);
                            if (q != null) {
                                result.addValidQuestion(q);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            result.addError("Failed to read DOCX document: " + e.getMessage());
        }

        return result;
    }
}
