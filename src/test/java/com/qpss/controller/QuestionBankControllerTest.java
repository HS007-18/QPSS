package com.qpss.controller;

import com.qpss.entity.Question;
import com.qpss.entity.Subject;
import com.qpss.service.QuestionBankService;
import com.qpss.service.SubjectService;
import com.qpss.util.QuestionContentSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuestionBankControllerTest {

    @Mock
    private QuestionBankService bankService;

    @Mock
    private SubjectService subjectService;

    @Mock
    private QuestionContentSanitizer contentSanitizer;

    @Mock
    private com.qpss.repository.QuestionRepository questionRepository;

    @InjectMocks
    private QuestionBankController questionBankController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testViewSubject_ReturnsDetailViewWithModel() {
        Long subjectId = 1L;
        Subject subject = Subject.builder().id(subjectId).code("CS101").name("Computer Science").build();
        Question q1 = Question.builder().id(10L).subjectId(subjectId).unit(1).t(1).marks(2).rbt("U").questionContent("What is RAM?").build();

        when(subjectService.findById(subjectId)).thenReturn(subject);
        when(bankService.getQuestionsBySubjectId(subjectId)).thenReturn(List.of(q1));
        when(contentSanitizer.sanitize(anyString())).thenAnswer(inv -> inv.getArgument(0));

        Model model = new ExtendedModelMap();
        String view = questionBankController.viewSubject(subjectId, model);

        assertEquals("subject/detail", view);
        assertEquals(subject, model.getAttribute("subject"));
        assertEquals(List.of(q1), model.getAttribute("questions"));
        assertNotNull(model.getAttribute("topicStats"));
    }

    @Test
    void testAddQuestion_ValidParameters_RedirectsToSubject() {
        Long subjectId = 1L;
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        when(contentSanitizer.sanitize(anyString())).thenAnswer(inv -> inv.getArgument(0));

        String view = questionBankController.addQuestion(
                subjectId, 1, "U", "CO1", 2, 1, "Sample content", redirect
        );

        assertEquals("redirect:/subjects/" + subjectId, view);
        assertEquals("Question added", redirect.getFlashAttributes().get("message"));
        verify(bankService).addQuestion(eq(subjectId), eq(1), eq("U"), eq("CO1"), eq(2), eq(1), isNull(), eq("Sample content"));
    }

    @Test
    void testAddQuestion_InvalidUnit_ThrowsException() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();
        assertThrows(IllegalArgumentException.class, () ->
                questionBankController.addQuestion(1L, 6, "U", "CO1", 2, 1, "Sample", redirect)
        );
    }
}
