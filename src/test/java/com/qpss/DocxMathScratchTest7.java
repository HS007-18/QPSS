package com.qpss;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.officeDocument.x2006.math.CTOMath;
import org.openxmlformats.schemas.officeDocument.x2006.math.CTOMathPara;

import java.io.FileInputStream;
import java.util.List;

public class DocxMathScratchTest7 {
    public static void main(String[] args) throws Exception {
        String path = "C:\\Users\\haris\\Downloads\\Part B QB Format.docx";
        try (FileInputStream fis = new FileInputStream(path);
             XWPFDocument doc = new XWPFDocument(fis)) {
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String text = cell.getText();
                        if (text.contains("Apply the concepts of mobile")) {
                            for (XWPFParagraph para : cell.getParagraphs()) {
                                CTP ctp = para.getCTP();
                                List<CTOMathPara> mathParas = ctp.getOMathParaList();
                                System.out.println("OMathPara list size: " + mathParas.size());
                                List<CTOMath> maths = ctp.getOMathList();
                                System.out.println("OMath list size: " + maths.size());
                                
                                for (CTOMathPara mp : mathParas) {
                                    for (CTOMath m : mp.getOMathList()) {
                                        System.out.println("MATH FROM OMathPara XML: " + m.xmlText());
                                    }
                                }
                                for (CTOMath m : maths) {
                                    System.out.println("MATH FROM OMath XML: " + m.xmlText());
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
