package com.qpss.service.parser;

import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

import java.util.List;

public class QuestionContentExtractor {

    public String extractPlainText(XWPFTableCell cell) {
        StringBuilder sb = new StringBuilder();
        List<XWPFParagraph> paragraphs = cell.getParagraphs();
        for (int i = 0; i < paragraphs.size(); i++) {
            sb.append(paragraphs.get(i).getText());
            if (i < paragraphs.size() - 1) {
                sb.append("<br/>");
            }
        }
        return sb.toString().trim();
    }

    public String extractRawOoxml(XWPFTableCell cell) {
        if (cell == null || cell.getCTTc() == null) {
            return null;
        }
        return cell.getCTTc().xmlText();
    }
}
