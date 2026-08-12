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
    void testFileStorageAndRetrieval() throws Exception {
        MultipartFile file = new MockMultipartFile("test.docx", "test.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "dummy content".getBytes());
        String storedFileName = storageService.storeDocument(file, ".docx");

        assertNotNull(storedFileName);
        assertTrue(storedFileName.endsWith(".docx"));

        Path retrieved = storageService.getDocument(storedFileName);
        assertTrue(Files.exists(retrieved));
        assertEquals("dummy content", Files.readString(retrieved));
    }

    @Test
    void testPathTraversalProtection() {
        assertThrows(SecurityException.class, () -> {
            storageService.getDocument("../outside.docx");
        });
    }

    @Test
    void testAbsolutePathProtection() {
        assertThrows(SecurityException.class, () -> {
            storageService.getDocument("/etc/passwd");
        });
    }

    @Test
    void testFileDeletion() throws Exception {
        MultipartFile file = new MockMultipartFile("test.docx", "test.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "dummy content".getBytes());
        String storedFileName = storageService.storeDocument(file, ".docx");

        Path retrieved = storageService.getDocument(storedFileName);
        assertTrue(Files.exists(retrieved));

        storageService.deleteDocument(storedFileName);
        assertFalse(Files.exists(retrieved));
    }
}
