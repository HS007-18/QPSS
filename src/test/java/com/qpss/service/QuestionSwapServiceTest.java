package com.qpss.service;

import com.qpss.entity.GeneratedPaper;
import com.qpss.entity.PaperQuestion;
import com.qpss.entity.Question;
import com.qpss.repository.GeneratedPaperRepository;
import com.qpss.repository.PaperQuestionRepository;
import com.qpss.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuestionSwapServiceTest {

    @Mock
    private GeneratedPaperRepository paperRepo;

    @Mock
    private PaperQuestionRepository paperQuestionRepo;

    @Mock
    private QuestionRepository questionRepo;

    @InjectMocks
    private QuestionSwapService swapService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSwapQuestion_Success() {
        Long paperId = 1L;
        Long oldQId = 10L;
        Long newQId = 20L;

        GeneratedPaper paper = GeneratedPaper.builder()
                .id(paperId)
                .sessionId(100L)
                .subjectId(5L)
                .build();

        PaperQuestion pq = PaperQuestion.builder()
                .paperId(paperId)
                .questionId(oldQId)
                .section("SECTION_A")
                .questionNumber(1)
                .build();

        Question oldQuestion = Question.builder()
                .id(oldQId)
                .subjectId(5L)
                .unit(1)
                .marks(2)
                .t(1)
                .rbt("U")
                .build();

        Question alternative = Question.builder()
                .id(newQId)
                .subjectId(5L)
                .unit(1)
                .marks(2)
                .t(1)
                .rbt("U")
                .build();

        when(paperRepo.findById(paperId)).thenReturn(Optional.of(paper));
        when(paperQuestionRepo.findByPaperIdOrderByQuestionNumberAscChoiceLabelAsc(paperId)).thenReturn(List.of(pq));
        when(questionRepo.findById(oldQId)).thenReturn(Optional.of(oldQuestion));
        when(paperRepo.findBySessionId(100L)).thenReturn(List.of(paper));
        when(paperQuestionRepo.findByPaperIdIn(anyList())).thenReturn(List.of(pq));
        when(questionRepo.findBySubjectIdOrderByUnitAscSerialNoAsc(5L)).thenReturn(List.of(oldQuestion, alternative));

        Question result = swapService.swapQuestion(paperId, oldQId);

        assertNotNull(result);
        assertEquals(newQId, result.getId());
        assertEquals(newQId, pq.getQuestionId());
        verify(paperQuestionRepo).save(pq);
    }

    @Test
    void testSwapQuestion_NoAlternative_ThrowsException() {
        Long paperId = 1L;
        Long oldQId = 10L;

        GeneratedPaper paper = GeneratedPaper.builder()
                .id(paperId)
                .sessionId(100L)
                .subjectId(5L)
                .build();

        PaperQuestion pq = PaperQuestion.builder()
                .paperId(paperId)
                .questionId(oldQId)
                .section("SECTION_A")
                .questionNumber(1)
                .build();

        Question oldQuestion = Question.builder()
                .id(oldQId)
                .subjectId(5L)
                .unit(1)
                .marks(2)
                .t(1)
                .rbt("U")
                .build();

        when(paperRepo.findById(paperId)).thenReturn(Optional.of(paper));
        when(paperQuestionRepo.findByPaperIdOrderByQuestionNumberAscChoiceLabelAsc(paperId)).thenReturn(List.of(pq));
        when(questionRepo.findById(oldQId)).thenReturn(Optional.of(oldQuestion));
        when(paperRepo.findBySessionId(100L)).thenReturn(List.of(paper));
        when(paperQuestionRepo.findByPaperIdIn(anyList())).thenReturn(List.of(pq));
        when(questionRepo.findBySubjectIdOrderByUnitAscSerialNoAsc(5L)).thenReturn(List.of(oldQuestion));

        assertThrows(IllegalStateException.class, () -> swapService.swapQuestion(paperId, oldQId));
    }
}
