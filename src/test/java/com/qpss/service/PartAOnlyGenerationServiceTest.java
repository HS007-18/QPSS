package com.qpss.service;

import com.qpss.entity.Question;
import com.qpss.exception.QuestionSelectionException;
import com.qpss.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PartAOnlyGenerationServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private PartAOnlyGenerationService partAOnlyGenService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGeneratePartASet_ArbitraryCountsAllowed() {
        Long subjectId = 1L;
        Question q1 = Question.builder().id(101L).unit(1).t(1).marks(2).build();
        Question q2 = Question.builder().id(102L).unit(1).t(2).marks(2).build();

        when(questionRepository.findBySubjectIdAndUnitAndMarks(subjectId, 1, 2))
                .thenReturn(List.of(q1, q2));

        // Requesting 2 questions total (not 50)
        Map<String, Integer> topicCounts = Map.of("1_1", 1, "1_2", 1);
        List<Question> selected = partAOnlyGenService.generatePartASet(subjectId, topicCounts);

        assertEquals(2, selected.size());
    }

    @Test
    void testGenerateQuestionsByCounts_20Marks() {
        Long subjectId = 1L;
        Question q1 = Question.builder().id(201L).unit(2).t(1).marks(20).build();
        Question q2 = Question.builder().id(202L).unit(2).t(2).marks(20).build();

        when(questionRepository.findBySubjectIdAndUnitAndMarks(subjectId, 2, 20))
                .thenReturn(List.of(q1, q2));

        Map<String, Integer> counts = Map.of("2_1", 1, "2_2", 1);
        List<Question> selected = partAOnlyGenService.generateQuestionsByCounts(subjectId, 20, counts);

        assertEquals(2, selected.size());
    }

    @Test
    void testGeneratePartASet_ZeroTotalThrowsException() {
        Long subjectId = 1L;
        Map<String, Integer> topicCounts = Collections.emptyMap();

        assertThrows(QuestionSelectionException.class, () ->
                partAOnlyGenService.generatePartASet(subjectId, topicCounts));
    }

    @Test
    void testGeneratePartASet_InsufficientQuestionsThrowsException() {
        Long subjectId = 1L;
        when(questionRepository.findBySubjectIdAndUnitAndMarks(subjectId, 1, 2))
                .thenReturn(Collections.emptyList());

        Map<String, Integer> topicCounts = Map.of("1_1", 2);

        assertThrows(QuestionSelectionException.class, () ->
                partAOnlyGenService.generatePartASet(subjectId, topicCounts));
    }
}
