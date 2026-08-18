package com.qpss;

import com.qpss.service.parser.QuestionContentExtractor;
import org.apache.poi.xwpf.usermodel.*;

import java.io.FileInputStream;

public class DocxMathScratchTest6 {
    public static void main(String[] args) throws Exception {
        String path = "C:\\Users\\haris\\Downloads\\Part B QB Format.docx";
        try (FileInputStream fis = new FileInputStream(path);
             XWPFDocument doc = new XWPFDocument(fis)) {
            QuestionContentExtractor extractor = new QuestionContentExtractor();
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String text = cell.getText();
                        if (text.contains("Apply the concepts of mobile")) {
                            String resultHtml = extractor.extractRichContent(cell, doc);
                            System.out.println("=== EXTRACTED RICH CONTENT HTML ===");
                            System.out.println(resultHtml);
                        }
                    }
                }
            }
        }
    }
}
