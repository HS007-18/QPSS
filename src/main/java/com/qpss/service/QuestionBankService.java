package com.qpss.service;

import com.qpss.model.Question;
import com.qpss.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionBankService {

    private final QuestionRepository repo;

    public Question addQuestion(Long subjectId, Long sessionId,
                                 int unit, String co, int marks,
                                 Integer serialNo, String content) {
        return repo.save(Question.builder()
                .subjectId(subjectId)
                .sessionId(sessionId)
                .unit(unit)
                .co(co)
                .marks(marks)
                .serialNo(serialNo)
                .questionContent(content)
                .build());
    }

    public void saveAll(List<Question> questions) {
        repo.saveAll(questions);
    }
}
