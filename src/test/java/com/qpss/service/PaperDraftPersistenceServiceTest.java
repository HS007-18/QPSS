package com.qpss.service;

import com.qpss.domain.selection.PairingEngine;
import com.qpss.entity.PaperQuestion;
import com.qpss.entity.Question;
import com.qpss.repository.GeneratedPaperRepository;
import com.qpss.repository.PaperQuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaperDraftPersistenceServiceTest {

    @Mock
    private GeneratedPaperRepository paperRepo;

    @Mock
    private PaperQuestionRepository paperQuestionRepo;

    @InjectMocks
    private PaperDraftPersistenceService draftPersistenceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testSavePaperQuestions_WhenNoTwoMarkQuestions_SavesPairsAsSectionAStartingFrom1() {
        Long paperId = 55L;
        Question choiceA = Question.builder().id(101L).unit(1).marks(20).build();
        Question choiceB = Question.builder().id(102L).unit(1).marks(20).build();
        PairingEngine.QuestionPair pair1 = new PairingEngine.QuestionPair(choiceA, choiceB, 1, 1);

        draftPersistenceService.savePaperQuestions(paperId, Collections.emptyList(), List.of(pair1), Collections.emptyMap());

        ArgumentCaptor<PaperQuestion> captor = ArgumentCaptor.forClass(PaperQuestion.class);
        verify(paperQuestionRepo, times(2)).save(captor.capture());

        List<PaperQuestion> saved = captor.getAllValues();
        assertEquals(2, saved.size());
        
        // Choice A
        assertEquals("SECTION_A", saved.get(0).getSection());
        assertEquals(1, saved.get(0).getQuestionNumber());
        assertEquals("a", saved.get(0).getChoiceLabel());

        // Choice B
        assertEquals("SECTION_A", saved.get(1).getSection());
        assertEquals(1, saved.get(1).getQuestionNumber());
        assertEquals("b", saved.get(1).getChoiceLabel());
    }

    @Test
    void testSavePaperQuestions_WithTwoMarkAndPairs_SavesSectionAAndSectionB() {
        Long paperId = 55L;
        Question q2m = Question.builder().id(1L).unit(1).marks(2).build();
        Question choiceA = Question.builder().id(101L).unit(1).marks(16).build();
        Question choiceB = Question.builder().id(102L).unit(1).marks(16).build();
        PairingEngine.QuestionPair pair1 = new PairingEngine.QuestionPair(choiceA, choiceB, 1, 1);

        draftPersistenceService.savePaperQuestions(paperId, List.of(q2m), List.of(pair1), Collections.emptyMap());

        ArgumentCaptor<PaperQuestion> captor = ArgumentCaptor.forClass(PaperQuestion.class);
        verify(paperQuestionRepo, times(3)).save(captor.capture());

        List<PaperQuestion> saved = captor.getAllValues();
        // 2M question
        assertEquals("SECTION_A", saved.get(0).getSection());
        assertEquals(1, saved.get(0).getQuestionNumber());

        // Pair 1 choices: numbered 2 (1 + pairIndex)
        assertEquals("SECTION_B", saved.get(1).getSection());
        assertEquals(2, saved.get(1).getQuestionNumber());
        assertEquals("a", saved.get(1).getChoiceLabel());

        assertEquals("SECTION_B", saved.get(2).getSection());
        assertEquals(2, saved.get(2).getQuestionNumber());
        assertEquals("b", saved.get(2).getChoiceLabel());
    }
}
