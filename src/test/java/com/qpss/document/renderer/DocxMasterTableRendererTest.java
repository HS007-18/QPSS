package com.qpss.document.renderer;


import com.qpss.entity.GeneratedPaper;

import com.qpss.repository.QuestionRepository;
import com.qpss.service.DocumentMetadataService;
import com.qpss.entity.Subject;
import org.apache.poi.xwpf.usermodel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class DocxMasterTableRendererTest {

    private DocxMasterTableRenderer masterTableRenderer;

    @BeforeEach
    void setUp() {
        QuestionRepository questionRepository = Mockito.mock(QuestionRepository.class);
        DocumentMetadataService metadataService = Mockito.mock(DocumentMetadataService.class);
        masterTableRenderer = new DocxMasterTableRenderer(questionRepository, metadataService);
    }

    @Test
    void testRenderMasterTable_HeaderRow0AndRow3Formatting() {
        XWPFDocument document = new XWPFDocument();
        GeneratedPaper paper = new GeneratedPaper();
        paper.setExamType("INTERNAL_1");

        Subject subject = new Subject();
        subject.setName("Computer Networks");

        masterTableRenderer.renderMasterTable(document, paper, subject, Collections.emptyList(), Collections.emptyList());

        assertFalse(document.getTables().isEmpty(), "Master table should be created");
        XWPFTable table = document.getTables().get(0);
        assertTrue(table.getRows().size() > 3, "Table should have header rows");

        // Row 0: Single merged cell containing a nested 2-column table
        XWPFTableRow r0 = table.getRow(0);
        String xml0 = r0.getCell(0).getCTTc().toString();

        assertTrue(xml0.contains("QUESTION PAPER CODE:"), "Row 0 must contain QUESTION PAPER CODE:");
        assertTrue(xml0.contains("REG.") && xml0.contains("NO.:"), "Row 0 must contain REG. NO.:");

        // Row 3: Academic Year
        XWPFTableRow r3 = table.getRow(3);
        String xml3 = r3.getCell(0).getCTTc().toString();

        assertTrue(xml3.contains("Academic Year:"), "Row 3 must contain Academic Year:");

        // Row 4: Date / Session
        XWPFTableRow r4 = table.getRow(4);
        String xml4 = r4.getCell(0).getCTTc().toString();

        assertTrue(xml4.contains("Date / Session:"), "Row 4 must contain Date / Session:");
    }
}
