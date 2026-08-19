package com.qpss.backend.questionbank;
import com.qpss.documentextraction.reader.DocxDocumentReader;
import com.qpss.documentextraction.model.ParsedQuestion;
import com.qpss.documentextraction.model.QuestionParseResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
@Service
public class QuestionParserService {

    private final QuestionContentSanitizer contentSanitizer;

    public QuestionParserService(QuestionContentSanitizer contentSanitizer) {
        this.contentSanitizer = contentSanitizer;
    }

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
                    .marksSplit(p.getMarksSplit())
                    .serialNo(p.getSerialNo())
                    .questionContent(contentSanitizer.sanitize(p.getQuestionContent()))
                    .sourceFileName(sourceFileName)
                    .t(p.getT())
                    .questionType(p.getQuestionType())
                    .build());
        }
        return questions;
    }
}