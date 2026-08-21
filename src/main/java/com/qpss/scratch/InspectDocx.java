package com.qpss.scratch;

import org.apache.poi.xwpf.usermodel.*;
import java.io.FileInputStream;
import java.io.File;

public class InspectDocx {
    public static void main(String[] args) throws Exception {
        String[] files = {
            "C:\\Users\\haris\\Downloads\\ESE Qn Format.docx",
            "C:\\Users\\haris\\Downloads\\CIA II Qn Format.docx",
            "C:\\Users\\haris\\Downloads\\CIA I Qn Format.docx"
        };

        for (String filePath : files) {
            System.out.println("==================================================");
            System.out.println("FILE: " + filePath);
            System.out.println("==================================================");
            File f = new File(filePath);
            if (!f.exists()) {
                System.out.println("File does not exist!");
                continue;
            }
            try (FileInputStream fis = new FileInputStream(f);
                 XWPFDocument doc = new XWPFDocument(fis)) {

                for (IBodyElement elem : doc.getBodyElements()) {
                    if (elem instanceof XWPFParagraph) {
                        XWPFParagraph p = (XWPFParagraph) elem;
                        String text = p.getText().trim();
                        if (!text.isEmpty()) {
                            System.out.println("[PARA] " + text);
                        }
                    } else if (elem instanceof XWPFTable) {
                        XWPFTable table = (XWPFTable) elem;
                        System.out.println("[TABLE (" + table.getRows().size() + " rows)]");
                        for (XWPFTableRow row : table.getRows()) {
                            StringBuilder sb = new StringBuilder("  | ");
                            for (XWPFTableCell cell : row.getTableCells()) {
                                sb.append(cell.getText().replace("\n", " ").trim()).append(" | ");
                            }
                            System.out.println(sb.toString());
                        }
                        System.out.println("[END TABLE]");
                    }
                }
            }
        }
    }
}
