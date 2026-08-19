package com.qpss.documentoutput.renderer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocxHeaderRendererTest {

    @Test
    void testCoInRangeInternal1KeepsCo1ToCo3() {
        assertTrue(DocxHeaderRenderer.coInRange("CO1", 1, 3));
        assertTrue(DocxHeaderRenderer.coInRange("CO2", 1, 3));
        assertTrue(DocxHeaderRenderer.coInRange("CO3", 1, 3));
        assertFalse(DocxHeaderRenderer.coInRange("CO4", 1, 3));
        assertFalse(DocxHeaderRenderer.coInRange("CO5", 1, 3));
    }

    @Test
    void testCoInRangeInternal2KeepsCo3ToCo5() {
        assertFalse(DocxHeaderRenderer.coInRange("CO1", 3, 5));
        assertFalse(DocxHeaderRenderer.coInRange("CO2", 3, 5));
        assertTrue(DocxHeaderRenderer.coInRange("CO3", 3, 5));
        assertTrue(DocxHeaderRenderer.coInRange("CO4", 3, 5));
        assertTrue(DocxHeaderRenderer.coInRange("CO5", 3, 5));
    }

    @Test
    void testCoInRangeRejectsMultiDigitCodes() {
        assertFalse(DocxHeaderRenderer.coInRange("CO10", 1, 3));
        assertFalse(DocxHeaderRenderer.coInRange("CO12", 1, 3));
        assertFalse(DocxHeaderRenderer.coInRange("CO23", 3, 5));
    }

    @Test
    void testCoInRangeRejectsBlankOrNull() {
        assertFalse(DocxHeaderRenderer.coInRange(null, 1, 3));
        assertFalse(DocxHeaderRenderer.coInRange("", 1, 3));
        assertFalse(DocxHeaderRenderer.coInRange("ABC", 1, 3));
    }
}