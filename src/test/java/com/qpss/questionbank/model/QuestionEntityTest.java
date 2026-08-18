package com.qpss.questionbank.model;

import com.qpss.questionbank.repository.QuestionRepository;
import com.qpss.session.model.Session;
import com.qpss.session.repository.SessionRepository;
import com.qpss.subject.model.Subject;
import com.qpss.subject.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class QuestionEntityTest {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private SessionRepository sessionRepository;

    private Long subjectId;
    private Long sessionId;

    @BeforeEach
    void setUp() {
        Subject subject = new Subject();
        subject.setName("Test Subject");
        subject = subjectRepository.save(subject);
        subjectId = subject.getId();

        Session session = new Session();
        session.setSubjectId(subjectId);
        session.setStatus("ACTIVE");
        session = sessionRepository.save(session);
        sessionId = session.getId();
    }

    @Test
    void testQuestionCanContainT1() {
        Question q = Question.builder()
                .subjectId(subjectId)
                .sessionId(sessionId)
                .unit(1)
                .co("CO1")
                .marks(16)
                .questionContent("Sample content")
                .t(1)
                .rbt("R")
                .build();

        Question saved = questionRepository.saveAndFlush(q);
        assertNotNull(saved.getId());
        assertEquals(1, saved.getT());
    }

    @Test
    void testQuestionCanContainT2() {
        Question q = Question.builder()
                .subjectId(subjectId)
                .sessionId(sessionId)
                .unit(2)
                .co("CO2")
                .marks(2)
                .questionContent("Sample content 2")
                .t(2)
                .rbt("U")
                .build();

        Question saved = questionRepository.saveAndFlush(q);
        assertNotNull(saved.getId());
        assertEquals(2, saved.getT());
    }

    @Test
    void testQuestionRejectsInvalidTValue() {
        Question q = Question.builder()
                .subjectId(subjectId)
                .sessionId(sessionId)
                .unit(1)
                .co("CO1")
                .marks(2)
                .questionContent("Sample content 3")
                .t(3)
                .rbt("R")
                .build();

        Exception thrown = assertThrows(Exception.class, () -> {
            questionRepository.saveAndFlush(q);
        });

        String errorMessage = thrown.getMessage();
        if (thrown.getCause() != null) {
            errorMessage += " " + thrown.getCause().getMessage();
            if (thrown.getCause().getCause() != null) {
                errorMessage += " " + thrown.getCause().getCause().getMessage();
            }
        }
        assertTrue(errorMessage.contains("exactly 1 or 2"));
    }

    @Test
    void testQuestionRejectsNullTValue() {
        Question q = Question.builder()
                .subjectId(subjectId)
                .sessionId(sessionId)
                .unit(1)
                .co("CO1")
                .marks(2)
                .questionContent("Sample content 4")
                .rbt("R")
                .build();

        Exception thrown = assertThrows(Exception.class, () -> {
            questionRepository.saveAndFlush(q);
        });

        String errorMessage = thrown.getMessage();
        if (thrown.getCause() != null) {
            errorMessage += " " + thrown.getCause().getMessage();
            if (thrown.getCause().getCause() != null) {
                errorMessage += " " + thrown.getCause().getCause().getMessage();
            }
        }
        assertTrue(errorMessage.contains("exactly 1 or 2"));
    }
}