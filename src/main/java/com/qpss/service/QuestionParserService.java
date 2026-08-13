package com.qpss.service;

import com.qpss.model.Question;
import com.qpss.service.parser.DocxDocumentReader;
import com.qpss.service.parser.ParsedQuestion;
import com.qpss.service.parser.QuestionParseResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionParserService {

    public QuestionParseResult parseDocx(MultipartFile file) throws IOException {
        return new DocxDocumentReader().parse(file.getInputStream(), file.getOriginalFilename());
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
                    .serialNo(p.getSerialNo())
                    .questionContent(p.getQuestionContent())
                    .sourceFileName(sourceFileName)
                    .t(p.getT())
                    .build());
        }
        return questions;
    }
}