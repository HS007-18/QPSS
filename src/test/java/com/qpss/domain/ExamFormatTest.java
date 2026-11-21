package com.qpss.domain;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExamFormatTest {

    @Test
    void testGetDisplayName() {
        assertEquals("2 Marks and 16 Marks", ExamFormat.FORMAT_1.getDisplayName());
        assertEquals("20 Marks", ExamFormat.FORMAT_2.getDisplayName());
        assertEquals("2 Marks Only", ExamFormat.FORMAT_3.getDisplayName());
    }

    @Test
    void testFrom() {
        assertEquals(ExamFormat.FORMAT_1, ExamFormat.from("FORMAT_1"));
        assertEquals(ExamFormat.FORMAT_2, ExamFormat.from("format_2"));
        assertEquals(ExamFormat.FORMAT_3, ExamFormat.from("FORMAT_3"));
        assertEquals(ExamFormat.FORMAT_1, ExamFormat.from(null));
        assertEquals(ExamFormat.FORMAT_1, ExamFormat.from(""));
        assertThrows(IllegalArgumentException.class, () -> ExamFormat.from("UNKNOWN"));
    }
}
