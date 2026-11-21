package com.qpss.document.parser;

import com.qpss.document.model.HeaderMetadata;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeaderMetadataExtractorTest {

    private final HeaderMetadataExtractor extractor = new HeaderMetadataExtractor();

    @Test
    void testExtractAcademicYear() {
        try (XWPFDocument document = new XWPFDocument()) {
            XWPFTable table = document.createTable(2, 2);
            XWPFTableRow row0 = table.getRow(0);
            row0.getCell(0).setText("KANGEYAM INSTITUTE OF TECHNOLOGY (Autonomous Institution)");
            row0.getCell(1).setText("QP CODE");

            XWPFTableRow row1 = table.getRow(1);
            row1.getCell(0).setText("Academic Year: 2026-27");
            row1.getCell(1).setText("");

            HeaderMetadata metadata = extractor.extract(document);
            assertEquals("2026-27", metadata.getAcademicYear());
            assertEquals("KANGEYAM INSTITUTE OF TECHNOLOGY", metadata.getInstitutionName());
            assertEquals("(Autonomous Institution)", metadata.getTagline());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
