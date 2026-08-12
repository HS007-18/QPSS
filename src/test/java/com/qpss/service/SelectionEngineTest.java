package com.qpss.service;

import com.qpss.model.Question;
import com.qpss.repository.QuestionRepository;
import com.qpss.service.distribution.DistributionPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class SelectionEngineTest {

    @Mock
    private QuestionRepository questionRepository;

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
                .questionContent("Sample")
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
            questions.add(q); // Add duplicate reference to same object (same ID)
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
        // 1. Exact unit allocation (4+4+2)
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
        assertEquals(10, q2m.size()); // 16. Global selected count

        assertEquals(4, q2m.stream().filter(q -> q.getUnit() == 1).count()); // 17, 18, 19 tests
        assertEquals(4, q2m.stream().filter(q -> q.getUnit() == 2).count());
        assertEquals(2, q2m.stream().filter(q -> q.getUnit() == 3).count());
    }

    @Test
    void testExactTAllocation() {
        // 2. Exact T allocation
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
        // 3. Unequal T allocation
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
        // 4. Zero T allocation
        long subId = 1L;
        long sessId = 1L;

        mockQuestions(subId, sessId, 1, 2, 1, 5);
        mockQuestions(subId, sessId, 1, 2, 2, 5); // T2 available

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(2).totalRequired(2).units(List.of(
                        makeUnitPlan(1, 2, 0)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(plan, subId, sessId);
        assertTrue(result.isSuccessful());
        
        List<Question> q2m = result.getTwoMarkQuestions();
        assertEquals(2, q2m.stream().filter(q -> q.getT() == 1).count());
        assertEquals(0, q2m.stream().filter(q -> q.getT() == 2).count()); // No T2 selected
    }

    @Test
    void testT1Shortage() {
        // 5. T1 shortage
        long subId = 1L;
        long sessId = 1L;

        mockQuestions(subId, sessId, 1, 2, 1, 1); // Only 1 available, but need 2
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
        // 6. T2 shortage
        long subId = 1L;
        long sessId = 1L;

        mockQuestions(subId, sessId, 1, 2, 1, 5);
        mockQuestions(subId, sessId, 1, 2, 2, 0); // None available, need 1

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
        // 7. No partial selection when ANY bucket is short
        long subId = 1L;
        long sessId = 1L;

        mockQuestions(subId, sessId, 1, 2, 1, 10);
        mockQuestions(subId, sessId, 1, 2, 2, 10);
        mockQuestions(subId, sessId, 2, 2, 1, 1); // SHORTAGE: available 1, need 2

        DistributionPlan plan = makePlan(List.of(
                DistributionPlan.SectionPlan.builder().marks(2).totalRequired(4).units(List.of(
                        makeUnitPlan(1, 2, 0),
                        makeUnitPlan(2, 2, 0)
                )).build()
        ));

        SelectionEngine.SelectionResult result = selectionEngine.select(plan, subId, sessId);
        assertFalse(result.isSuccessful());
        assertEquals(0, result.getTwoMarkQuestions().size()); // Zero selected
        assertEquals(0, result.getSixteenMarkQuestions().size()); // Zero selected
    }

    @Test
    void testDuplicateQuestionIdProtection() {
        // 8. Duplicate Question ID protection (repository returning duplicate IDs)
        // 24. Repository returning duplicate Question IDs
        long subId = 1L;
        long sessId = 1L;

        // Will return 3 unique questions, but each duplicated in the list (total 6 items, 3 unique)
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
        // Same as above but only 1 unique question available, need 2
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
        assertEquals(1, result.getShortages().get(0).getAvailable()); // Recognized only 1 unique available
    }

    @Test
    void testSubjectSessionMarksUnitTIsolation() {
        // Tests 9, 10, 11, 12, 13 (Isolation)
        // We verify isolation inherently because we mock the exact DB call by arguments.
        // If the engine queried for a wrong subject/session, it would get null/empty from mock 
        // and fail due to shortage. Thus, successful completion implies correct isolation parameters.
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
        // 14. CO independence
        // In `mockQuestions` we set CO to CO1, CO2, etc. 
        // We never pass CO into the engine or DB mock query, so it's fully independent.
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
        // 15. Both 2M and 16M sections together
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
        List<Question> q16m = result.getSixteenMarkQuestions();
        
        assertEquals(2, q2m.size());
        assertEquals(2, q2m.get(0).getMarks());
        
        assertEquals(2, q16m.size());
        assertEquals(16, q16m.get(0).getMarks());
    }

    @Test
    void testInvalidPlanHandling() {
        // 20. Invalid/null DistributionPlan handling
        assertThrows(IllegalArgumentException.class, () -> {
            selectionEngine.select(null, 1L, 1L);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            selectionEngine.select(makePlan(new ArrayList<>()), 1L, 1L);
        });
    }

    @Test
    void testEmptyCandidatePool() {
        // 23. Empty candidate pool handling
        long subId = 1L;
        long sessId = 1L;

        // DB returns nothing
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
}
