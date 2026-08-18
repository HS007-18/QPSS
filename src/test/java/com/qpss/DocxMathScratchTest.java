package com.qpss;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

import java.io.FileInputStream;

public class DocxMathScratchTest {
    public static void main(String[] args) throws Exception {
        String path = "C:\\Users\\haris\\Downloads\\Part B QB Format.docx";
        try (FileInputStream fis = new FileInputStream(path);
             XWPFDocument doc = new XWPFDocument(fis)) {
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String text = cell.getText();
                        if (text.contains("Apply the concepts of mobile")) {
                            System.out.println("CELL TEXT: " + text);
                            for (XWPFParagraph para : cell.getParagraphs()) {
                                System.out.println("PARA getText(): " + para.getText());
                                System.out.println("CTP XML: " + para.getCTP().xmlText());
                            }
                        }
                    }
                }
            }
        }
    }
}
