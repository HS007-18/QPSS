package com.qpss;

import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

import java.io.FileInputStream;

public class DocxReaderScratch {
    public static void main(String[] args) {
        String[] files = {
            "C:\\Users\\haris\\Downloads\\Part A QB Format.docx",
            "C:\\Users\\haris\\Downloads\\Part B QB Format.docx"
        };
        
        for (String file : files) {
            System.out.println("==================================================");
            System.out.println("READING FILE: " + file);
            System.out.println("==================================================");
            try (XWPFDocument doc = new XWPFDocument(new FileInputStream(file))) {
                for (IBodyElement element : doc.getBodyElements()) {
                    if (element instanceof XWPFParagraph) {
                        System.out.println("PARAGRAPH: " + ((XWPFParagraph) element).getText());
                    } else if (element instanceof XWPFTable) {
                        XWPFTable table = (XWPFTable) element;
                        System.out.println("TABLE (" + table.getNumberOfRows() + " rows):");
                        for (XWPFTableRow row : table.getRows()) {
                            System.out.print("  ROW: ");
                            for (XWPFTableCell cell : row.getTableCells()) {
                                System.out.print("[" + cell.getText() + "] ");
                            }
                            System.out.println();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
