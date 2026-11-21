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

        // Row 4: Academic Year
        XWPFTableRow r4 = table.getRow(4);
        String xml4 = r4.getCell(0).getCTTc().toString();

        assertTrue(xml4.contains("Academic Year:"), "Row 4 must contain Academic Year:");

        // Row 5: Date / Session
        XWPFTableRow r5 = table.getRow(5);
        String xml5 = r5.getCell(0).getCTTc().toString();

        assertTrue(xml5.contains("Date / Session:"), "Row 5 must contain Date / Session:");
    }

    @Test
    void testRenderMasterTable_DurationRowHasBlankMaximumMarks() {
        XWPFDocument document = new XWPFDocument();
        GeneratedPaper paper = new GeneratedPaper();
        paper.setDuration("Two Hours");
        paper.setExamType("INTERNAL_1");

        Subject subject = new Subject();
        subject.setName("Operating Systems");

        masterTableRenderer.renderMasterTable(document, paper, subject, Collections.emptyList(), Collections.emptyList());

        XWPFTable table = document.getTables().get(0);
        boolean foundDurationRow = false;
        for (XWPFTableRow row : table.getRows()) {
            String xml = row.getCell(0).getCTTc().toString();
            if (xml.contains("Duration: Two Hours")) {
                foundDurationRow = true;
                assertTrue(xml.contains("Maximum Marks:"), "Must contain Maximum Marks label");
                assertFalse(xml.contains("Maximum Marks:\u00A0100"), "Maximum marks must not be hardcoded to 100");
            }
        }
        assertTrue(foundDurationRow, "Duration row must be present in master table");
    }

    @Test
    void testRenderMasterTable_SingleSection20MRendersAsPartA() {
        QuestionRepository mockRepo = Mockito.mock(QuestionRepository.class);
        DocumentMetadataService mockMeta = Mockito.mock(DocumentMetadataService.class);
        DocxMasterTableRenderer renderer = new DocxMasterTableRenderer(mockRepo, mockMeta);

        com.qpss.entity.Question q20a = com.qpss.entity.Question.builder()
                .id(101L).unit(1).marks(20).questionContent("Explain Process Scheduling").build();
        com.qpss.entity.Question q20b = com.qpss.entity.Question.builder()
                .id(102L).unit(1).marks(20).questionContent("Explain Memory Management").build();

        Mockito.when(mockRepo.findById(101L)).thenReturn(java.util.Optional.of(q20a));
        Mockito.when(mockRepo.findById(102L)).thenReturn(java.util.Optional.of(q20b));

        com.qpss.entity.PaperQuestion pq1 = com.qpss.entity.PaperQuestion.builder()
                .paperId(1L).questionId(101L).section("SECTION_A").questionNumber(1).choiceLabel("a").build();
        com.qpss.entity.PaperQuestion pq2 = com.qpss.entity.PaperQuestion.builder()
                .paperId(1L).questionId(102L).section("SECTION_A").questionNumber(1).choiceLabel("b").build();

        XWPFDocument document = new XWPFDocument();
        GeneratedPaper paper = new GeneratedPaper();
        paper.setDuration("Two Hours");
        paper.setExamType("SEMESTER");

        Subject subject = new Subject();
        subject.setName("Operating Systems");

        renderer.renderMasterTable(document, paper, subject, java.util.List.of(pq1, pq2), Collections.emptyList());

        XWPFTable table = document.getTables().get(0);
        String allXml = table.getCTTbl().toString();

        assertTrue(allXml.contains("PART - A (1 x 20 = 20 Marks)"), "Must render PART - A for 20M single section");
        assertTrue(allXml.contains("1. (a)"), "Must render 1.(a)");
        assertTrue(allXml.contains("OR"), "Must render OR between choices");
        assertFalse(allXml.contains("PART - B"), "Single section must not render PART - B");
    }
}
