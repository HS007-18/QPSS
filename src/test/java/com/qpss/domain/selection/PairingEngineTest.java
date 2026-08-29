package com.qpss.domain.selection;

import com.qpss.domain.selection.PairingEngine;
import com.qpss.entity.Question;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PairingEngineTest {

    private final PairingEngine pairingEngine = new PairingEngine();

    private long idCounter = 1L;

    private Question question(int unit, int t, String rbt) {
        return Question.builder()
                .id(idCounter++)
                .subjectId(1L)
                .sessionId(1L)
                .unit(unit)
                .marks(16)
                .co("CO1")
                .t(t)
                .rbt(rbt)
                .questionContent("Q")
                .build();
    }

    @Test
    void testSameHalfPairsGroupByUnitAndT() {
        List<Question> questions = new ArrayList<>();
        questions.add(question(1, 1, "U"));
        questions.add(question(1, 1, "U"));
        questions.add(question(1, 2, "U"));
        questions.add(question(1, 2, "U"));
        questions.add(question(2, 1, "AP"));
        questions.add(question(2, 1, "AP"));

        List<PairingEngine.QuestionPair> pairs = pairingEngine.createPairs(questions, PairingEngine.PairingMode.SAME_HALF);

        assertEquals(3, pairs.size());
        assertEquals(1, pairs.get(0).getPairIndex());
        assertEquals(1, pairs.get(0).getUnit());
        assertEquals(1, pairs.get(0).getChoiceA().getT());
        assertEquals(1, pairs.get(0).getChoiceB().getT());
        assertEquals(2, pairs.get(1).getPairIndex());
        assertEquals(1, pairs.get(1).getUnit());
        assertEquals(2, pairs.get(1).getChoiceA().getT());
        assertEquals(2, pairs.get(1).getChoiceB().getT());
        assertEquals(3, pairs.get(2).getPairIndex());
        assertEquals(2, pairs.get(2).getUnit());
        assertEquals(1, pairs.get(2).getChoiceA().getT());
        assertEquals(1, pairs.get(2).getChoiceB().getT());
        assertNotEquals(pairs.get(0).getChoiceA().getId(), pairs.get(0).getChoiceB().getId());
    }

    @Test
    void testCrossHalfPairsChoiceAIsT1ChoiceBIsT2() {
        List<Question> questions = new ArrayList<>();
        questions.add(question(1, 2, "U"));
        questions.add(question(1, 1, "U"));
        questions.add(question(2, 1, "AP"));
        questions.add(question(2, 2, "AP"));
        questions.add(question(3, 2, "AZ"));
        questions.add(question(3, 1, "AZ"));

        List<PairingEngine.QuestionPair> pairs = pairingEngine.createPairs(questions, PairingEngine.PairingMode.CROSS_HALF);

        assertEquals(3, pairs.size());
        assertEquals(1, pairs.get(0).getPairIndex());
        assertEquals(1, pairs.get(0).getUnit());
        assertEquals(1, pairs.get(0).getChoiceA().getT());
        assertEquals(2, pairs.get(0).getChoiceB().getT());
        assertEquals(2, pairs.get(1).getUnit());
        assertEquals(1, pairs.get(1).getChoiceA().getT());
        assertEquals(2, pairs.get(1).getChoiceB().getT());
        assertEquals(3, pairs.get(2).getUnit());
        assertEquals(1, pairs.get(2).getChoiceA().getT());
        assertEquals(2, pairs.get(2).getChoiceB().getT());
    }

    @Test
    void testCrossHalfPairsRejectUnequalHalves() {
        List<Question> questions = new ArrayList<>();
        questions.add(question(1, 1, "U"));
        questions.add(question(1, 1, "U"));
        questions.add(question(1, 2, "U"));

        assertThrows(IllegalStateException.class,
                () -> pairingEngine.createPairs(questions, PairingEngine.PairingMode.CROSS_HALF));
    }

    @Test
    void testOddCountRejected() {
        List<Question> questions = new ArrayList<>();
        questions.add(question(1, 1, "U"));
        questions.add(question(1, 1, "U"));
        questions.add(question(1, 2, "U"));

        assertThrows(IllegalStateException.class,
                () -> pairingEngine.createPairs(questions, PairingEngine.PairingMode.SAME_HALF));
    }

    @Test
    void testSameHalfPairsShareRbtWhenAvailable() {
        List<Question> questions = new ArrayList<>();
        questions.add(question(1, 1, "U"));
        questions.add(question(1, 1, "U"));
        questions.add(question(1, 1, "AP"));
        questions.add(question(1, 1, "AP"));

        List<PairingEngine.QuestionPair> pairs = pairingEngine.createPairs(questions, PairingEngine.PairingMode.SAME_HALF);

        assertEquals(2, pairs.size());
        assertEquals(pairs.get(0).getChoiceA().getRbt(), pairs.get(0).getChoiceB().getRbt());
    }
}
