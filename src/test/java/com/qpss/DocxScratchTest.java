package com.qpss;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

import java.io.FileInputStream;

public class DocxScratchTest {
    public static void main(String[] args) throws Exception {
        String path = "C:\\Users\\haris\\Downloads\\Part A QB Format.docx";
        try (FileInputStream fis = new FileInputStream(path);
             XWPFDocument doc = new XWPFDocument(fis)) {
            for (XWPFTable table : doc.getTables()) {
                System.out.println("--- TABLE ---");
                for (XWPFTableRow row : table.getRows()) {
                    System.out.print("ROW: ");
                    for (XWPFTableCell cell : row.getTableCells()) {
                        System.out.print("[" + cell.getText() + "] ");
                    }
                    System.out.println();
                }
            }
        }
    }
}
