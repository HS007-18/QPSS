package com.qpss.backend.questionbank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
        String storedFileName = storageService.storeDocument("dummy content".getBytes(), ".docx");

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
        String storedFileName = storageService.storeDocument("dummy content".getBytes(), ".docx");

        Path stored = tempDir.resolve(storedFileName);
        assertTrue(Files.exists(stored));

        storageService.deleteDocument(storedFileName);
        assertFalse(Files.exists(stored));
    }
}