package com.qpss;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

import java.io.FileInputStream;

public class DocxMathScratchTest3 {
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
                                    System.out.println("FULL CTP XML:\n" + xml);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
