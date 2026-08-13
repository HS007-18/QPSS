package com.qpss.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class SourceDocumentStorageServiceTest {

    @TempDir
    Path tempDir;

    private SourceDocumentStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new SourceDocumentStorageService(tempDir.toString());
        storageService.init();
    }

    @Test
    void testFileStorage() throws Exception {
        MultipartFile file = new MockMultipartFile("test.docx", "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "dummy content".getBytes());
        String storedFileName = storageService.storeDocument(file, ".docx");

        assertNotNull(storedFileName);
        assertTrue(storedFileName.endsWith(".docx"));

        Path stored = tempDir.resolve(storedFileName);
        assertTrue(Files.exists(stored));
        assertEquals("dummy content", Files.readString(stored));
    }

    @Test
    void testPathTraversalProtection() {
        assertThrows(SecurityException.class, () -> {
            storageService.deleteDocument("../outside.docx");
        });
    }

    @Test
    void testAbsolutePathProtection() {
        assertThrows(SecurityException.class, () -> {
            storageService.deleteDocument("/etc/passwd");
        });
    }

    @Test
    void testFileDeletion() throws Exception {
        MultipartFile file = new MockMultipartFile("test.docx", "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "dummy content".getBytes());
        String storedFileName = storageService.storeDocument(file, ".docx");

        Path stored = tempDir.resolve(storedFileName);
        assertTrue(Files.exists(stored));

        storageService.deleteDocument(storedFileName);
        assertFalse(Files.exists(stored));
    }
}