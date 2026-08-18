package com.qpss.backend.questionbank;

import com.qpss.documentextraction.model.ParsedQuestion;
import com.qpss.documentextraction.model.QuestionFields;
import com.qpss.documentextraction.model.QuestionParseResult;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestionParserServiceTest {

    private final QuestionParserService parserService = new QuestionParserService(new QuestionContentSanitizer());

    @Test
    void testParseDocx_withFiveColumnTable() throws IOException {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph p = document.createParagraph();
        p.createRun().setText("UNIT 1");

        XWPFTable table = document.createTable();
        XWPFTableRow header = table.getRow(0);
        header.getCell(0).setText("S.No");
        header.addNewTableCell().setText("Questions");
        header.addNewTableCell().setText("M");
        header.addNewTableCell().setText("CO");
        header.addNewTableCell().setText("I / II Half");

        XWPFTableRow row = table.createRow();
        row.getCell(0).setText("1");
        row.getCell(1).setText("Define AI.");
        row.getCell(2).setText("2");
        row.getCell(3).setText("CO1");
        row.getCell(4).setText("1");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.write(out);
        document.close();

        MultipartFile file = new MockMultipartFile("file", "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", out.toByteArray());
        QuestionParseResult result = parserService.parseDocx(file);

        assertTrue(result.getErrors().isEmpty());
        assertEquals(1, result.getValidQuestions().size());
        ParsedQuestion q = result.getValidQuestions().get(0);
        assertEquals(1, q.getSerialNo());
        assertTrue(q.getQuestionContent().contains("Define AI."));
        assertEquals(2, q.getMarks());
        assertEquals("CO1", q.getCo());
        assertEquals(1, q.getT());
        assertEquals(1, q.getUnit());
        assertNull(q.getRbt());
        assertFalse(q.isComplete());
        assertEquals(List.of(QuestionFields.RBT), q.missingFields());
    }

    @Test
    void testParseDocx_withSixColumnTableIncludingRbt() throws IOException {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph p = document.createParagraph();
        p.createRun().setText("UNIT 2");

        XWPFTable table = document.createTable();
        XWPFTableRow header = table.getRow(0);
        header.getCell(0).setText("S.No");
        header.addNewTableCell().setText("Questions");
        header.addNewTableCell().setText("M");
        header.addNewTableCell().setText("RBT");
        header.addNewTableCell().setText("CO");
        header.addNewTableCell().setText("I / II");

        XWPFTableRow row = table.createRow();
        row.getCell(0).setText("1");
        row.getCell(1).setText("Explain BFS.");
        row.getCell(2).setText("16");
        row.getCell(3).setText("U");
        row.getCell(4).setText("CO2");
        row.getCell(5).setText("II");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.write(out);
        document.close();

        MultipartFile file = new MockMultipartFile("file", "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", out.toByteArray());
        QuestionParseResult result = parserService.parseDocx(file);

        assertTrue(result.getErrors().isEmpty());
        assertEquals(1, result.getValidQuestions().size());
        ParsedQuestion q = result.getValidQuestions().get(0);
        assertEquals(16, q.getMarks());
        assertEquals("U", q.getRbt());
        assertEquals("CO2", q.getCo());
        assertEquals(2, q.getT());
        assertEquals(2, q.getUnit());
        assertTrue(q.isComplete());
    }

    @Test
    void testParseDocx_withMultipleTablesRenumbersSerialsGlobally() throws IOException {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph p = document.createParagraph();
        p.createRun().setText("UNIT 1");

        XWPFTable table1 = document.createTable();
        XWPFTableRow header1 = table1.getRow(0);
        header1.getCell(0).setText("S.No");
        header1.addNewTableCell().setText("Questions");
        header1.addNewTableCell().setText("M");
        header1.addNewTableCell().setText("CO");
        header1.addNewTableCell().setText("I / II Half");
        XWPFTableRow row1 = table1.createRow();
        row1.getCell(0).setText("1");
        row1.getCell(1).setText("Question A");
        row1.getCell(2).setText("2");
        row1.getCell(3).setText("CO1");
        row1.getCell(4).setText("I");

        XWPFTable table2 = document.createTable();
        XWPFTableRow header2 = table2.getRow(0);
        header2.getCell(0).setText("S.No");
        header2.addNewTableCell().setText("Questions");
        header2.addNewTableCell().setText("M");
        header2.addNewTableCell().setText("CO");
        header2.addNewTableCell().setText("I / II Half");
        XWPFTableRow row2 = table2.createRow();
        row2.getCell(0).setText("1");
        row2.getCell(1).setText("Question B");
        row2.getCell(2).setText("16");
        row2.getCell(3).setText("CO1");
        row2.getCell(4).setText("II");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.write(out);
        document.close();

        MultipartFile file = new MockMultipartFile("file", "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", out.toByteArray());
        QuestionParseResult result = parserService.parseDocx(file);

        assertTrue(result.getErrors().isEmpty());
        assertEquals(2, result.getValidQuestions().size());
        ParsedQuestion first = result.getValidQuestions().get(0);
        ParsedQuestion second = result.getValidQuestions().get(1);
        assertEquals(1, first.getSerialNo());
        assertEquals(2, second.getSerialNo());
        assertEquals(2, first.getMarks());
        assertEquals(16, second.getMarks());
    }

    @Test
    void testParseDocx_withSplitMarksSumsToTotal() throws IOException {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph p = document.createParagraph();
        p.createRun().setText("UNIT 1");

        XWPFTable table = document.createTable();
        XWPFTableRow header = table.getRow(0);
        header.getCell(0).setText("S.No");
        header.addNewTableCell().setText("Questions");
        header.addNewTableCell().setText("M");
        header.addNewTableCell().setText("CO");
        header.addNewTableCell().setText("I / II Half");

        XWPFTableRow row1 = table.createRow();
        row1.getCell(0).setText("1");
        row1.getCell(1).setText("Question A");
        row1.getCell(2).setText("8+8");
        row1.getCell(3).setText("CO1");
        row1.getCell(4).setText("I");
        XWPFTableRow row2 = table.createRow();
        row2.getCell(0).setText("2");
        row2.getCell(1).setText("Question B");
        row2.getCell(2).setText("10+6");
        row2.getCell(3).setText("CO1");
        row2.getCell(4).setText("I");
        XWPFTableRow row3 = table.createRow();
        row3.getCell(0).setText("3");
        row3.getCell(1).setText("Question C");
        row3.getCell(2).setText("10 + 10");
        row3.getCell(3).setText("CO1");
        row3.getCell(4).setText("I");
        XWPFTableRow row4 = table.createRow();
        row4.getCell(0).setText("4");
        row4.getCell(1).setText("Question D");
        row4.getCell(2).setText("7+7");
        row4.getCell(3).setText("CO1");
        row4.getCell(4).setText("I");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.write(out);
        document.close();

        MultipartFile file = new MockMultipartFile("file", "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", out.toByteArray());
        QuestionParseResult result = parserService.parseDocx(file);

        assertTrue(result.getErrors().isEmpty());
        assertEquals(4, result.getValidQuestions().size());
        assertEquals(16, result.getValidQuestions().get(0).getMarks());
        assertEquals(16, result.getValidQuestions().get(1).getMarks());
        assertEquals(20, result.getValidQuestions().get(2).getMarks());
        assertNull(result.getValidQuestions().get(3).getMarks());
        assertEquals("8+8", result.getValidQuestions().get(0).getMarksSplit());
        assertEquals("10+6", result.getValidQuestions().get(1).getMarksSplit());
        assertEquals("10+10", result.getValidQuestions().get(2).getMarksSplit());
        assertNull(result.getValidQuestions().get(3).getMarksSplit());
    }

    @Test
    void testParseDocx_contentWithRomanNumeralPartsPreserved() throws IOException {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph p = document.createParagraph();
        p.createRun().setText("UNIT 1");

        XWPFTable table = document.createTable();
        XWPFTableRow header = table.getRow(0);
        header.getCell(0).setText("S.No");
        header.addNewTableCell().setText("Questions");
        header.addNewTableCell().setText("M");
        header.addNewTableCell().setText("CO");
        header.addNewTableCell().setText("I / II Half");

        String content = "Explain agents. (i) Describe architecture. (ii) Illustrate working with a diagram. "
                + "(iii) Compare with a model-based agent. (iv) State limitations.";
        XWPFTableRow row = table.createRow();
        row.getCell(0).setText("1");
        row.getCell(1).setText(content);
        row.getCell(2).setText("8+8");
        row.getCell(3).setText("CO1");
        row.getCell(4).setText("I");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.write(out);
        document.close();

        MultipartFile file = new MockMultipartFile("file", "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", out.toByteArray());
        QuestionParseResult result = parserService.parseDocx(file);

        assertTrue(result.getErrors().isEmpty());
        assertEquals(1, result.getValidQuestions().size());
        ParsedQuestion q = result.getValidQuestions().get(0);
        assertEquals(16, q.getMarks());
        assertTrue(q.getQuestionContent().contains("(i) Describe architecture."));
        assertTrue(q.getQuestionContent().contains("(ii) Illustrate working with a diagram."));
        assertTrue(q.getQuestionContent().contains("(iii) Compare with a model-based agent."));
        assertTrue(q.getQuestionContent().contains("(iv) State limitations."));
    }

    @Test
    void testParseDocx_breakInContentEmitsBr() throws IOException {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph p = document.createParagraph();
        p.createRun().setText("UNIT 1");

        XWPFTable table = document.createTable();
        XWPFTableRow header = table.getRow(0);
        header.getCell(0).setText("S.No");
        header.addNewTableCell().setText("Questions");
        header.addNewTableCell().setText("M");
        header.addNewTableCell().setText("CO");
        header.addNewTableCell().setText("I / II Half");

        XWPFTableRow row = table.createRow();
        row.getCell(0).setText("1");
        XWPFParagraph contentPara = row.getCell(1).getParagraphs().get(0);
        XWPFRun run = contentPara.createRun();
        run.setText("(a) Explain the architecture. ");
        XWPFRun breakRun = contentPara.createRun();
        breakRun.addBreak();
        breakRun.setText("(b) Illustrate with a diagram.");
        row.getCell(2).setText("8+8");
        row.getCell(3).setText("CO1");
        row.getCell(4).setText("I");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.write(out);
        document.close();

        MultipartFile file = new MockMultipartFile("file", "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", out.toByteArray());
        QuestionParseResult result = parserService.parseDocx(file);

        assertTrue(result.getErrors().isEmpty());
        assertEquals(1, result.getValidQuestions().size());
        ParsedQuestion q = result.getValidQuestions().get(0);
        assertTrue(q.getQuestionContent().contains("<br/>"));
        assertTrue(q.getQuestionContent().contains("(a) Explain the architecture."));
        assertTrue(q.getQuestionContent().contains("(b) Illustrate with a diagram."));
        assertEquals(16, q.getMarks());
        assertEquals("8+8", q.getMarksSplit());
    }

    @Test
    void testToQuestions_mapping() {
        ParsedQuestion pq = ParsedQuestion.builder()
                .serialNo(1)
                .questionContent("Q")
                .marks(16)
                .marksSplit("8+8")
                .co("CO2")
                .t(2)
                .unit(2)
                .rbt("R")
                .build();

        List<Question> questions = parserService.toQuestions(Collections.singletonList(pq), 1L, 2L, 3L, "file.docx");

        assertEquals(1, questions.size());
        Question q = questions.get(0);
        assertEquals(1L, q.getSubjectId());
        assertEquals(2L, q.getSessionId());
        assertEquals(3L, q.getSourceDocumentId());
        assertEquals("file.docx", q.getSourceFileName());
        assertEquals(1, q.getSerialNo());
        assertEquals("Q", q.getQuestionContent());
        assertEquals(16, q.getMarks());
        assertEquals("8+8", q.getMarksSplit());
        assertEquals("CO2", q.getCo());
        assertEquals(2, q.getT());
        assertEquals(2, q.getUnit());
        assertEquals("R", q.getRbt());
    }
}