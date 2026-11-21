package com.qpss.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuestionContentSanitizerTest {

    private final QuestionContentSanitizer sanitizer = new QuestionContentSanitizer();

    @Test
    void testSanitize_RemovesScriptTags() {
        String input = "<p>What is polymorphism? <script>alert('xss')</script></p>";
        String cleaned = sanitizer.sanitize(input);
        assertFalse(cleaned.contains("<script>"));
        assertTrue(cleaned.contains("What is polymorphism?"));
    }

    @Test
    void testSanitize_RemovesEventHandlers() {
        String input = "<button onclick=\"doSomething()\" onmouseover=\"bad()\">Click me</button>";
        String cleaned = sanitizer.sanitize(input);
        assertFalse(cleaned.contains("onclick"));
        assertFalse(cleaned.contains("onmouseover"));
    }

    @Test
    void testSanitize_AllowsBase64Image() {
        String input = "<img src=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=\" alt=\"test\">";
        String cleaned = sanitizer.sanitize(input);
        assertTrue(cleaned.contains("data:image/png;base64"));
        assertTrue(cleaned.contains("alt=\"test\""));
    }

    @Test
    void testSanitize_RemovesExternalImageProtocol() {
        String input = "<img src=\"http://malicious-site.com/image.png\" alt=\"bad\">";
        String cleaned = sanitizer.sanitize(input);
        assertFalse(cleaned.contains("http://malicious-site.com"));
    }

    @Test
    void testSanitize_NullAndBlank() {
        assertNull(sanitizer.sanitize(null));
        assertEquals("", sanitizer.sanitize(""));
        assertEquals("   ", sanitizer.sanitize("   "));
    }
}
