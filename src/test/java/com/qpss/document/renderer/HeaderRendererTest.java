package com.qpss.document.renderer;

import com.qpss.document.model.HeaderMetadata;
import com.qpss.entity.GeneratedPaper;
import com.qpss.entity.Subject;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HeaderRendererTest {

    @Test
    void testRenderHeaderBlock_WithCustomAcademicYearAndTagline() {
        HeaderRenderer renderer = new HeaderRenderer("2025-26");

        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFTable table = doc.createTable(1, 5);

            GeneratedPaper paper = GeneratedPaper.builder()
                    .id(1L)
                    .examType("INTERNAL_1")
                    .build();

            Subject subject = Subject.builder()
                    .id(10L)
                    .name("Operating Systems")
                    .build();

            HeaderMetadata metadata = HeaderMetadata.builder()
                    .institutionName("KIT TECH")
                    .tagline("(Deemed University)")
                    .academicYear("2027-28")
                    .build();

            renderer.renderHeaderBlock(table, 0, paper, subject, metadata);

            boolean hasTagline = false;
            boolean hasAcademicYear = false;

            for (XWPFTableRow row : table.getRows()) {
                String xml = row.getCell(0).getCTTc().toString();
                if (xml.contains("(Deemed University)")) {
                    hasTagline = true;
                }
                if (xml.contains("Academic Year: 2027-28")) {
                    hasAcademicYear = true;
                }
            }

            assertTrue(hasTagline, "Row must contain custom tagline");
            assertTrue(hasAcademicYear, "Row must contain custom Academic Year");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testRenderHeaderBlock_FallbackAcademicYearAndTagline() {
        HeaderRenderer renderer = new HeaderRenderer("2025-26");

        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFTable table = doc.createTable(1, 5);

            GeneratedPaper paper = GeneratedPaper.builder()
                    .id(1L)
                    .examType("SEMESTER")
                    .build();

            Subject subject = Subject.builder()
                    .id(10L)
                    .name("Data Structures")
                    .build();

            HeaderMetadata metadata = HeaderMetadata.builder().build();

            renderer.renderHeaderBlock(table, 0, paper, subject, metadata);

            boolean hasTagline = false;
            boolean hasAcademicYear = false;

            for (XWPFTableRow row : table.getRows()) {
                String xml = row.getCell(0).getCTTc().toString();
                if (xml.contains("(Autonomous Institution)")) {
                    hasTagline = true;
                }
                if (xml.contains("Academic Year: 2025-26")) {
                    hasAcademicYear = true;
                }
            }

            assertTrue(hasTagline, "Row must fall back to default tagline");
            assertTrue(hasAcademicYear, "Row must fall back to configured Academic Year");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
