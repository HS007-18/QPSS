package com.qpss.service;

import com.qpss.entity.Question;
import com.qpss.util.QuestionContentSanitizer;
import com.qpss.document.parser.DocxDocumentReader;
import com.qpss.document.model.ParsedQuestion;
import com.qpss.document.model.QuestionParseResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
@Service
public class QuestionParserService {

    private final QuestionContentSanitizer contentSanitizer;
    private final DocxDocumentReader docxDocumentReader;

    public QuestionParserService(QuestionContentSanitizer contentSanitizer, DocxDocumentReader docxDocumentReader) {
        this.contentSanitizer = contentSanitizer;
        this.docxDocumentReader = docxDocumentReader;
    }

    public QuestionParseResult parseDocx(MultipartFile file) throws IOException {
        return docxDocumentReader.parse(file.getInputStream(), file.getOriginalFilename());
    }


    public List<Question> toQuestions(List<ParsedQuestion> parsed, Long subjectId,
                                      Long sessionId, Long sourceDocumentId, String sourceFileName) {
        List<Question> questions = new ArrayList<>();
        for (ParsedQuestion p : parsed) {
            questions.add(Question.builder()
                    .subjectId(subjectId)
                    .sessionId(sessionId)
                    .sourceDocumentId(sourceDocumentId)
                    .unit(p.getUnit())
                    .rbt(p.getRbt())
                    .co(p.getCo())
                    .marks(p.getMarks())
                    .marksSplit(p.getMarksSplit())
                    .serialNo(p.getSerialNo())
                    .questionContent(contentSanitizer.sanitize(p.getQuestionContent()))
                    .structuredContent(p.getStructuredContent())
                    .sourceFileName(sourceFileName)
                    .t(p.getT())
                    .questionType(p.getQuestionType())
                    .build());
        }
        return questions;
    }
}