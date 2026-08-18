package com.qpss.backend.questionbank;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class QuestionBankService {

    private final QuestionRepository repo;

    public Question addQuestion(Long subjectId, Long sessionId,
                                 int unit, String rbt, String co, int marks, int t,
                                 Integer serialNo, String content) {
        return repo.save(Question.builder()
                .subjectId(subjectId)
                .sessionId(sessionId)
                .unit(unit)
                .rbt(rbt)
                .co(co)
                .marks(marks)
                .t(t)
                .serialNo(serialNo)
                .questionContent(content)
                .build());
    }
}