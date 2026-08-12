package com.qpss.service;

import com.qpss.model.Question;
import com.qpss.service.parser.ParsedQuestion;
import com.qpss.service.parser.QuestionParseResult;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
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

    private final QuestionParserService parserService = new QuestionParserService();

    @Test
    void testParseDocx_withValidTable() throws IOException {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph p = document.createParagraph();
        p.createRun().setText("UNIT 1");
        
        XWPFTable table = document.createTable();
        XWPFTableRow header = table.getRow(0);
        header.getCell(0).setText("S.No");
        header.addNewTableCell().setText("Questions");
        header.addNewTableCell().setText("M");
        header.addNewTableCell().setText("CO");
        header.addNewTableCell().setText("T");
        
        XWPFTableRow row = table.createRow();
        row.getCell(0).setText("1");
        row.getCell(1).setText("Define AI.");
        row.getCell(2).setText("2");
        row.getCell(3).setText("CO1");
        row.getCell(4).setText("1");
        
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.write(out);
        document.close();
        
        MultipartFile file = new MockMultipartFile("file", "test.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", out.toByteArray());
        QuestionParseResult result = parserService.parseDocx(file);
        
        assertFalse(result.hasErrors());
        assertEquals(1, result.getValidQuestions().size());
        ParsedQuestion q = result.getValidQuestions().get(0);
        assertEquals(1, q.getSerialNo());
        assertEquals("Define AI.", q.getQuestionContent());
        assertEquals(2, q.getMarks());
        assertEquals("CO1", q.getCo());
        assertEquals(1, q.getT());
        assertEquals(1, q.getUnit());
    }

    @Test
    void testToQuestions_mapping() {
        ParsedQuestion pq = ParsedQuestion.builder()
                .serialNo(1)
                .questionContent("Q")
                .rawOoxml("<xml>")
                .marks(16)
                .co("CO2")
                .t(2)
                .unit(2)
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
        assertEquals("<xml>", q.getRawOoxml());
        assertEquals(16, q.getMarks());
        assertEquals("CO2", q.getCo());
        assertEquals(2, q.getT());
        assertEquals(2, q.getUnit());
    }
}
