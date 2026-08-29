package com.qpss.document.parser;
import com.qpss.document.model.ParsedQuestion;
import com.qpss.document.model.QuestionParseResult;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
@Component
public class DocxDocumentReader {

    private final QuestionTableDetector tableDetector = new QuestionTableDetector();
    private final QuestionRowParser rowParser = new QuestionRowParser();

    public QuestionParseResult parse(InputStream inputStream, String filename) throws IOException {
        QuestionParseResult result = new QuestionParseResult();
        UnitContextResolver unitResolver = new UnitContextResolver(filename);

        try (InputStream is = inputStream; XWPFDocument doc = new XWPFDocument(is)) {
            List<IBodyElement> bodyElements = doc.getBodyElements();
            for (IBodyElement element : bodyElements) {
                if (element instanceof XWPFParagraph) {
                    unitResolver.processParagraph(((XWPFParagraph) element).getText());
                } else if (element instanceof XWPFTable) {
                    XWPFTable table = (XWPFTable) element;
                    XWPFTableRow headerRow = tableDetector.findHeaderRow(table);
                    if (headerRow == null) {
                        continue;
                    }
                    ColumnLayout layout = new ColumnLayout(headerRow);
                    boolean passedHeader = false;
                    for (XWPFTableRow row : table.getRows()) {
                        if (row == headerRow) {
                            passedHeader = true;
                            continue;
                        }
                        if (!passedHeader) {
                            continue;
                        }
                        try {
                            ParsedQuestion q = rowParser.parseRow(row, layout, unitResolver.getCurrentUnit(), result, doc);
                            if (q != null) {
                                result.addValidQuestion(q);
                            }
                        } catch (Exception e) {
                            result.addError("Row " + table.getRows().indexOf(row) + ": " + e.getMessage());
                        }
                    }
                }
            }
            List<ParsedQuestion> validQuestions = result.getValidQuestions();
            for (int i = 0; i < validQuestions.size(); i++) {
                validQuestions.get(i).setSerialNo(i + 1);
            }
        } catch (Exception e) {
            result.addError("Failed to read DOCX document: " + e.getMessage());
        }

        return result;
    }
}