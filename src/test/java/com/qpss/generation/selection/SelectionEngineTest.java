package com.qpss.generation.selection;

import com.qpss.questionbank.model.Question;
import com.qpss.questionbank.repository.QuestionRepository;
import com.qpss.generation.distribution.DistributionPlan;
import com.qpss.generation.selection.PairingEngine.PairingMode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class SelectionEngineTest {

    @Mock
    private QuestionRepository questionRepository;

    @Spy
    private RbtPairPicker pairPicker = new RbtPairPicker();

    @InjectMocks
    private SelectionEngine selectionEngine;

    private long questionIdCounter = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private Question makeQuestion(long subjectId, long sessionId, int unit, int marks, int t, String co) {
        return Question.builder()
                .id(questionIdCounter++)
                .subjectId(subjectId)
                .sessionId(sessionId)
                .unit(unit)
                .marks(marks)
                .t(t)
                .co(co)
                .questionContent("Sample Q" + questionIdCounter)
                .build();
    }

    private void mockQuestions(long subjectId, long sessionId, int unit, int marks, int t, int count) {
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            questions.add(makeQuestion(subjectId, sessionId, unit, marks, t, "CO" + (i % 5 + 1)));
        }
        when(questionRepository.findBySubjectIdAndSessionIdAndUnitAndMarksAndT(
                eq(subjectId), eq(sessionId), eq(unit), eq(marks), eq(t)))
                .thenReturn(questions);
    }

    private void mockQuestionsWithDuplicates(long subjectId, long sessionId, int unit, int marks, int t, int uniqueCount) {
        List<Question> questions = new ArrayList<>();
        for (int i = 0; i < uniqueCount; i++) {
            Question q = makeQuestion(subjectId, sessionId, unit, marks, t, "CO1");
            questions.add(q);
            questions.add(q);
        }
        when(questionRepository.findBySubjectIdAndSessionIdAndUnitAndMarksAndT(
                eq(subjectId), eq(sessionId), eq(unit), eq(marks), eq(t)))
                .thenReturn(questions);
    }

    private DistributionPlan.UnitPlan makeUnitPlan(int unit, int t1Req, int t2Req) {
        return DistributionPlan.UnitPlan.builder()
                .unit(unit)
                .requiredCount(t1Req + t2Req)
                .t1Required(t1Req)
                .t2Required(t2Req)
                .percentage(BigDecimal.ZERO)
                .t1Percentage(BigDecimal.ZERO)
                .t2Percentage(BigDecimal.ZERO)
                .build();
    }

    private DistributionPlan makePlan(List<DistributionPlan.SectionPlan> sections) {
        return DistributionPlan.builder()
                .examType("TEST")
                .sections(sections)
                .build();
    }

    @Test
    void testExactUnitAllocation_4_4_2() {

        long subId = 1L;
        long sessId = 1L;

        mockQuestions(subId, sessId, 1, 2, 1, 10);
        mockQuestions(subId, sessId, 1, 2, 2, 10);
        mockQuestions(subId, sessId, 2, 2, 1, 10);
        mockQuestions(subId, sessId, 2, 2, 2, 10);
        mockQuestions(subId, sessId, 3, 2, 1, 10);
        mockQuestions(subId, sessId, 3, 2, 2, 10);

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(2).totalRequired(10).units(List.of(
                        makeUnitPlan(1, 2, 2),
                        makeUnitPlan(2, 2, 2),
                        makeUnitPlan(3, 1, 1)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(plan, subId, sessId);
        assertTrue(result.isSuccessful());

        List<Question> q2m = result.getTwoMarkQuestions();
        assertEquals(10, q2m.size());

        assertEquals(4, q2m.stream().filter(q -> q.getUnit() == 1).count());
        assertEquals(4, q2m.stream().filter(q -> q.getUnit() == 2).count());
        assertEquals(2, q2m.stream().filter(q -> q.getUnit() == 3).count());
    }

    @Test
    void testExactTAllocation() {

        long subId = 1L;
        long sessId = 1L;

        mockQuestions(subId, sessId, 1, 2, 1, 5);
        mockQuestions(subId, sessId, 1, 2, 2, 5);

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(2).totalRequired(4).units(List.of(
                        makeUnitPlan(1, 2, 2)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(plan, subId, sessId);
        assertTrue(result.isSuccessful());

        List<Question> q2m = result.getTwoMarkQuestions();
        assertEquals(2, q2m.stream().filter(q -> q.getT() == 1).count());
        assertEquals(2, q2m.stream().filter(q -> q.getT() == 2).count());
    }

    @Test
    void testUnequalTAllocation() {

        long subId = 1L;
        long sessId = 1L;

        mockQuestions(subId, sessId, 1, 2, 1, 5);
        mockQuestions(subId, sessId, 1, 2, 2, 5);

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(2).totalRequired(3).units(List.of(
                        makeUnitPlan(1, 2, 1)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(plan, subId, sessId);
        assertTrue(result.isSuccessful());

        List<Question> q2m = result.getTwoMarkQuestions();
        assertEquals(2, q2m.stream().filter(q -> q.getT() == 1).count());
        assertEquals(1, q2m.stream().filter(q -> q.getT() == 2).count());
    }

    @Test
    void testZeroTAllocation() {

        long subId = 1L;
        long sessId = 1L;

        mockQuestions(subId, sessId, 1, 2, 1, 5);
        mockQuestions(subId, sessId, 1, 2, 2, 5);

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(2).totalRequired(2).units(List.of(
                        makeUnitPlan(1, 2, 0)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(plan, subId, sessId);
        assertTrue(result.isSuccessful());

        List<Question> q2m = result.getTwoMarkQuestions();
        assertEquals(2, q2m.stream().filter(q -> q.getT() == 1).count());
        assertEquals(0, q2m.stream().filter(q -> q.getT() == 2).count());
    }

    @Test
    void testT1Shortage() {

        long subId = 1L;
        long sessId = 1L;

        mockQuestions(subId, sessId, 1, 2, 1, 1);
        mockQuestions(subId, sessId, 1, 2, 2, 5);

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(2).totalRequired(4).units(List.of(
                        makeUnitPlan(1, 2, 2)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(plan, subId, sessId);
        assertFalse(result.isSuccessful());
        assertEquals(1, result.getShortages().size());

        SelectionEngine.SelectionShortage shortage = result.getShortages().get(0);
        assertEquals(1, shortage.getUnit());
        assertEquals(2, shortage.getMarks());
        assertEquals(1, shortage.getT());
        assertEquals(2, shortage.getRequired());
        assertEquals(1, shortage.getAvailable());
        assertEquals(1, shortage.getShortage());
    }

    @Test
    void testT2Shortage() {

        long subId = 1L;
        long sessId = 1L;

        mockQuestions(subId, sessId, 1, 2, 1, 5);
        mockQuestions(subId, sessId, 1, 2, 2, 0);

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(2).totalRequired(3).units(List.of(
                        makeUnitPlan(1, 2, 1)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(plan, subId, sessId);
        assertFalse(result.isSuccessful());
        assertEquals(1, result.getShortages().size());
        assertEquals(2, result.getShortages().get(0).getT());
    }

    @Test
    void testNoPartialSelection() {

        long subId = 1L;
        long sessId = 1L;

        mockQuestions(subId, sessId, 1, 2, 1, 10);
        mockQuestions(subId, sessId, 1, 2, 2, 10);
        mockQuestions(subId, sessId, 2, 2, 1, 1);

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(2).totalRequired(4).units(List.of(
                        makeUnitPlan(1, 2, 0),
                        makeUnitPlan(2, 2, 0)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(plan, subId, sessId);
        assertFalse(result.isSuccessful());
        assertEquals(0, result.getTwoMarkQuestions().size());
        assertEquals(0, result.getQuestionsByMarks(16).size());
    }

    @Test
    void testDuplicateQuestionIdProtection() {

        long subId = 1L;
        long sessId = 1L;

        mockQuestionsWithDuplicates(subId, sessId, 1, 2, 1, 3);

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(2).totalRequired(2).units(List.of(
                        makeUnitPlan(1, 2, 0)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(plan, subId, sessId);
        assertTrue(result.isSuccessful());
        assertEquals(2, result.getTwoMarkQuestions().size());
        assertNotEquals(result.getTwoMarkQuestions().get(0).getId(), result.getTwoMarkQuestions().get(1).getId());
    }

    @Test
    void testDuplicateQuestionIdCausesShortage() {

        long subId = 1L;
        long sessId = 1L;

        mockQuestionsWithDuplicates(subId, sessId, 1, 2, 1, 1);

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(2).totalRequired(2).units(List.of(
                        makeUnitPlan(1, 2, 0)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(plan, subId, sessId);
        assertFalse(result.isSuccessful());
        assertEquals(1, result.getShortages().size());
        assertEquals(1, result.getShortages().get(0).getAvailable());
    }

    @Test
    void testSubjectSessionMarksUnitTIsolation() {

        long subId = 99L;
        long sessId = 88L;

        mockQuestions(subId, sessId, 1, 2, 1, 5);

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(2).totalRequired(2).units(List.of(
                        makeUnitPlan(1, 2, 0)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(plan, subId, sessId);
        assertTrue(result.isSuccessful());
    }

    @Test
    void testCOIndependence() {

        long subId = 1L;
        long sessId = 1L;

        mockQuestions(subId, sessId, 1, 2, 1, 5);

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(2).totalRequired(2).units(List.of(
                        makeUnitPlan(1, 2, 0)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(plan, subId, sessId);
        assertTrue(result.isSuccessful());
        assertEquals(2, result.getTwoMarkQuestions().size());
    }

    @Test
    void testBothSections() {

        long subId = 1L;
        long sessId = 1L;

        mockQuestions(subId, sessId, 1, 2, 1, 5);
        mockQuestions(subId, sessId, 2, 16, 2, 5);

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(2).totalRequired(2).units(List.of(
                        makeUnitPlan(1, 2, 0)
                )).build(),
                DistributionPlan.SectionPlan.builder().marks(16).totalRequired(2).units(List.of(
                        makeUnitPlan(2, 0, 2)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(plan, subId, sessId);
        assertTrue(result.isSuccessful());

        List<Question> q2m = result.getTwoMarkQuestions();
        List<Question> q16m = result.getQuestionsByMarks(16);

        assertEquals(2, q2m.size());
        assertEquals(2, q2m.get(0).getMarks());

        assertEquals(2, q16m.size());
        assertEquals(16, q16m.get(0).getMarks());
    }

    @Test
    void testInvalidPlanHandling() {

        assertThrows(IllegalArgumentException.class, () -> {
            selectionEngine.select(null, 1L, 1L);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            selectionEngine.select(makePlan(new ArrayList<>()), 1L, 1L);
        });
    }

    @Test
    void testEmptyCandidatePool() {

        long subId = 1L;
        long sessId = 1L;

        when(questionRepository.findBySubjectIdAndSessionIdAndUnitAndMarksAndT(
                eq(subId), eq(sessId), eq(1), eq(2), eq(1)))
                .thenReturn(new ArrayList<>());

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(2).totalRequired(2).units(List.of(
                        makeUnitPlan(1, 2, 0)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(plan, subId, sessId);
        assertFalse(result.isSuccessful());
        assertEquals(0, result.getShortages().get(0).getAvailable());
        assertEquals(2, result.getShortages().get(0).getShortage());
    }

    @Test
    void testSameHalfSelectionPairsShareRbt() {

        long subId = 1L;
        long sessId = 1L;

        mockQuestionsWithRbt(subId, sessId, 1, 16, 1, "U", "U", "AP", "AP");
        mockQuestionsWithRbt(subId, sessId, 1, 16, 2, "U", "U", "AP", "AP");

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(16).totalRequired(4).units(List.of(
                        makeUnitPlan(1, 2, 2)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(
                plan, subId, sessId, java.util.Collections.emptySet(), PairingMode.SAME_HALF);

        assertTrue(result.isSuccessful());

        List<Question> t1 = result.getQuestionsByMarks(16).stream()
                .filter(q -> q.getT() == 1).collect(Collectors.toList());
        List<Question> t2 = result.getQuestionsByMarks(16).stream()
                .filter(q -> q.getT() == 2).collect(Collectors.toList());
        assertEquals(2, t1.size());
        assertEquals(2, t2.size());
        assertEquals(t1.get(0).getRbt(), t1.get(1).getRbt());
        assertEquals(t2.get(0).getRbt(), t2.get(1).getRbt());
    }

    @Test
    void testSameHalfSelectionFallsBackWhenNoSameRbtPairAvailable() {

        long subId = 1L;
        long sessId = 1L;

        mockQuestionsWithRbt(subId, sessId, 1, 16, 1, "U", "U", "U", "AP");
        mockQuestionsWithRbt(subId, sessId, 1, 16, 2, "U", "U", "AP", "AP");

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(16).totalRequired(4).units(List.of(
                        makeUnitPlan(1, 2, 2)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(
                plan, subId, sessId, java.util.Collections.emptySet(), PairingMode.SAME_HALF);

        assertTrue(result.isSuccessful());
        assertEquals(4, result.getQuestionsByMarks(16).size());
    }

    @Test
    void testCrossHalfSelectionMatchesRbtBetweenHalves() {

        long subId = 1L;
        long sessId = 1L;

        mockQuestionsWithRbt(subId, sessId, 1, 16, 1, "R", "U");
        mockQuestionsWithRbt(subId, sessId, 1, 16, 2, "R", "U");
        mockQuestionsWithRbt(subId, sessId, 2, 16, 1, "AP", "AZ");
        mockQuestionsWithRbt(subId, sessId, 2, 16, 2, "AP", "AZ");

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(16).totalRequired(4).units(List.of(
                        makeUnitPlan(1, 1, 1),
                        makeUnitPlan(2, 1, 1)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(
                plan, subId, sessId, java.util.Collections.emptySet(), PairingMode.CROSS_HALF);

        assertTrue(result.isSuccessful());

        List<Question> unit1T1 = result.getQuestionsByMarks(16).stream()
                .filter(q -> q.getUnit() == 1 && q.getT() == 1).collect(Collectors.toList());
        List<Question> unit1T2 = result.getQuestionsByMarks(16).stream()
                .filter(q -> q.getUnit() == 1 && q.getT() == 2).collect(Collectors.toList());
        List<Question> unit2T1 = result.getQuestionsByMarks(16).stream()
                .filter(q -> q.getUnit() == 2 && q.getT() == 1).collect(Collectors.toList());
        List<Question> unit2T2 = result.getQuestionsByMarks(16).stream()
                .filter(q -> q.getUnit() == 2 && q.getT() == 2).collect(Collectors.toList());

        assertEquals(1, unit1T1.size());
        assertEquals(1, unit1T2.size());
        assertEquals(1, unit2T1.size());
        assertEquals(1, unit2T2.size());
        assertEquals(unit1T1.get(0).getRbt(), unit1T2.get(0).getRbt());
        assertEquals(unit2T1.get(0).getRbt(), unit2T2.get(0).getRbt());
    }

    @Test
    void testSameHalfSelectionNoDuplicateIdsOnDistinctRbtFallback() {

        long subId = 1L;
        long sessId = 1L;

        mockQuestionsWithRbt(subId, sessId, 1, 16, 1, "R", "U", "AP", "AZ");
        mockQuestionsWithRbt(subId, sessId, 1, 16, 2, "R", "U", "AP", "AZ");

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(16).totalRequired(4).units(List.of(
                        makeUnitPlan(1, 2, 2)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(
                plan, subId, sessId, java.util.Collections.emptySet(), PairingMode.SAME_HALF);

        assertTrue(result.isSuccessful());
        List<Question> selected = result.getQuestionsByMarks(16);
        assertEquals(4, selected.size());
        assertEquals(4, selected.stream().map(Question::getId).distinct().count());
    }

    @Test
    void testSameHalfSelectionOddRequiredNoDuplicateIds() {

        long subId = 1L;
        long sessId = 1L;

        mockQuestionsWithRbt(subId, sessId, 1, 16, 1, "U", "U", "U", "U", "U");
        mockQuestionsWithRbt(subId, sessId, 1, 16, 2, "U", "U", "U", "U", "U");

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(16).totalRequired(5).units(List.of(
                        makeUnitPlan(1, 3, 2)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(
                plan, subId, sessId, java.util.Collections.emptySet(), PairingMode.SAME_HALF);

        assertTrue(result.isSuccessful());
        List<Question> selected = result.getQuestionsByMarks(16);
        assertEquals(5, selected.size());
        assertEquals(5, selected.stream().map(Question::getId).distinct().count());
    }

    private void mockQuestionsWithRbt(long subjectId, long sessionId, int unit, int marks, int t, String... rbts) {
        List<Question> questions = new ArrayList<>();
        for (String rbt : rbts) {
            questions.add(makeQuestionWithRbt(subjectId, sessionId, unit, marks, t, rbt));
        }
        when(questionRepository.findBySubjectIdAndSessionIdAndUnitAndMarksAndT(
                eq(subjectId), eq(sessionId), eq(unit), eq(marks), eq(t)))
                .thenReturn(questions);
    }

    private Question makeQuestionWithRbt(long subjectId, long sessionId, int unit, int marks, int t, String rbt) {
        Question q = makeQuestion(subjectId, sessionId, unit, marks, t, "CO" + (questionIdCounter % 5 + 1));
        q.setRbt(rbt);
        return q;
    }
}
