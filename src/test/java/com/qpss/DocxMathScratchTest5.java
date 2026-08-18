package com.qpss;

import com.qpss.service.parser.QuestionContentExtractor;
import org.apache.poi.xwpf.usermodel.*;

import java.io.FileInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocxMathScratchTest5 {
    public static void main(String[] args) throws Exception {
        String path = "C:\\Users\\haris\\Downloads\\Part B QB Format.docx";
        try (FileInputStream fis = new FileInputStream(path);
             XWPFDocument doc = new XWPFDocument(fis)) {
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText();
                        if (cellText.contains("Apply the concepts of mobile")) {
                            for (XWPFParagraph para : cell.getParagraphs()) {
                                String xml = para.getCTP().xmlText();
                                if (xml.contains("m:oMath")) {
                                    Pattern mathPattern = Pattern.compile("<m:oMath[^>]*>(.*?)</m:oMath>", Pattern.DOTALL);
                                    Matcher matcher = mathPattern.matcher(xml);
                                    while (matcher.find()) {
                                        String fullMathXml = "<m:oMath xmlns:m=\"http://schemas.openxmlformats.org/officeDocument/2006/math\">" + matcher.group(1) + "</m:oMath>";
                                        try {
                                            javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                                            dbf.setNamespaceAware(true);
                                            javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
                                            db.parse(new java.io.ByteArrayInputStream(fullMathXml.getBytes("UTF-8")));
                                            System.out.println("PARSED OK!");
                                        } catch (Exception e) {
                                            System.out.println("PARSER ERROR: " + e.getMessage());
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
