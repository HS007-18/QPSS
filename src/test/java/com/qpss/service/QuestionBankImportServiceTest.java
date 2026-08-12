package com.qpss.service;

import com.qpss.model.QuestionBankImport;
import com.qpss.model.SourceDocument;
import com.qpss.repository.QuestionBankImportRepository;
import com.qpss.repository.QuestionRepository;
import com.qpss.repository.SourceDocumentRepository;
import com.qpss.service.parser.QuestionParseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class QuestionBankImportServiceTest {

    @Mock
    private QuestionBankImportRepository importRepository;

    @Mock
    private SourceDocumentRepository sourceDocumentRepository;

    @Mock
    private SourceDocumentStorageService storageService;

    @Mock
    private QuestionParserService parserService;

    @Mock
    private QuestionRepository questionRepository;

    private QuestionBankImportService importService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        importService = new QuestionBankImportService(importRepository, sourceDocumentRepository, storageService, parserService, questionRepository);
    }

    @Test
    void testValidDocxUpload() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "valid.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "content".getBytes());

        QuestionBankImport importBatch = new QuestionBankImport();
        importBatch.setId(1L);
        when(importRepository.save(any())).thenReturn(importBatch);
        when(storageService.storeDocument(any(), any())).thenReturn("uuid.docx");
        
        QuestionParseResult parseResult = new QuestionParseResult();
        when(parserService.parseDocx(any())).thenReturn(parseResult);
        when(parserService.toQuestions(any(), any(), any(), any(), any())).thenReturn(Collections.emptyList());

        // Mock save returning an entity with ID so the parsing step doesn't NPE
        when(sourceDocumentRepository.save(any(SourceDocument.class))).thenAnswer(invocation -> {
            SourceDocument sd = invocation.getArgument(0);
            sd.setId(100L);
            return sd;
        });
        
        QuestionBankImportResult result = importService.createImportBatch(10L, 20L, Collections.singletonList(file));

        assertNotNull(result);
        assertTrue(result.isSuccessful());
        verify(importRepository).save(any());
        verify(storageService).storeDocument(eq(file), eq(".docx"));
        verify(parserService).parseDocx(eq(file));
        verify(questionRepository).saveAll(any());
    }

    @Test
    void testDuplicateFileDetectionWithinBatch() throws IOException {
        MockMultipartFile file1 = new MockMultipartFile("file1", "valid.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "same content".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file2", "valid2.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "same content".getBytes());

        QuestionBankImport importBatch = new QuestionBankImport();
        importBatch.setId(1L);
        when(importRepository.save(any())).thenReturn(importBatch);
        when(storageService.storeDocument(any(), any())).thenReturn("uuid.docx");
        when(sourceDocumentRepository.existsByImportBatchIdAndChecksum(anyLong(), anyString())).thenReturn(false);

        QuestionParseResult parseResult = new QuestionParseResult();
        when(parserService.parseDocx(any())).thenReturn(parseResult);

        when(sourceDocumentRepository.save(any(SourceDocument.class))).thenAnswer(invocation -> {
            SourceDocument sd = invocation.getArgument(0);
            sd.setId(100L);
            return sd;
        });

        QuestionBankImportResult result = importService.createImportBatch(10L, 20L, Arrays.asList(file1, file2));

        // Only one should be saved because the second has the exact same content (checksum)
        verify(storageService, times(1)).storeDocument(any(), any());
        
        verify(sourceDocumentRepository, times(1)).save(any(SourceDocument.class));
        assertTrue(result.isSuccessful());
    }

    @Test
    void testRejectsInvalidExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "invalid.pdf", "application/pdf", "content".getBytes());

        assertThrows(IllegalArgumentException.class, () -> {
            importService.createImportBatch(10L, 20L, Collections.singletonList(file));
        });
    }

    @Test
    void testUppercaseExtensionAcceptance() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "UPPER.DOCX", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "content".getBytes());
        
        QuestionBankImport importBatch = new QuestionBankImport();
        importBatch.setId(1L);
        when(importRepository.save(any())).thenReturn(importBatch);
        when(storageService.storeDocument(any(), any())).thenReturn("uuid.docx");
        when(sourceDocumentRepository.save(any(SourceDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        QuestionParseResult parseResult = new QuestionParseResult();
        when(parserService.parseDocx(any())).thenReturn(parseResult);

        QuestionBankImportResult result = importService.createImportBatch(10L, 20L, Collections.singletonList(file));
        assertTrue(result.isSuccessful());
    }

    @Test
    void testDatabaseFailureCleanup() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "valid.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "content".getBytes());

        QuestionBankImport importBatch = new QuestionBankImport();
        importBatch.setId(1L);
        when(importRepository.save(any())).thenReturn(importBatch);
        when(storageService.storeDocument(any(), any())).thenReturn("uuid.docx");
        
        QuestionParseResult parseResult = new QuestionParseResult();
        when(parserService.parseDocx(any())).thenReturn(parseResult);

        when(sourceDocumentRepository.save(any(SourceDocument.class))).thenAnswer(invocation -> {
            SourceDocument sd = invocation.getArgument(0);
            sd.setId(100L);
            return sd;
        });

        // Simulate DB failure on questions
        when(questionRepository.saveAll(any())).thenThrow(new RuntimeException("DB Failed"));

        assertThrows(RuntimeException.class, () -> {
            importService.createImportBatch(10L, 20L, Collections.singletonList(file));
        });

        // Verify cleanup was called
        verify(storageService).deleteDocument("uuid.docx");
    }

    @Test
    void testEmptyFileRejection() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> {
            importService.createImportBatch(10L, 20L, Collections.singletonList(file));
        });
    }

    @Test
    void testBatchParsingFailureAbortsEverything() throws IOException {
        MockMultipartFile file1 = new MockMultipartFile("file1", "valid1.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "content 1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file2", "invalid.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "content 2".getBytes());
        MockMultipartFile file3 = new MockMultipartFile("file3", "valid3.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "content 3".getBytes());

        QuestionParseResult validResult1 = new QuestionParseResult();
        QuestionParseResult invalidResult2 = new QuestionParseResult();
        invalidResult2.addError("Missing T column");
        QuestionParseResult validResult3 = new QuestionParseResult();

        when(parserService.parseDocx(eq(file1))).thenReturn(validResult1);
        when(parserService.parseDocx(eq(file2))).thenReturn(invalidResult2);
        when(parserService.parseDocx(eq(file3))).thenReturn(validResult3);

        QuestionBankImportResult result = importService.createImportBatch(10L, 20L, Arrays.asList(file1, file2, file3));

        // 1. Result should indicate failure
        assertFalse(result.isSuccessful());
        assertEquals(1, result.getParsingErrors().size());
        assertTrue(result.getParsingErrors().get(0).contains("invalid.docx: Missing T column"));

        // 2. ALL 3 files should have been parsed to check for errors
        verify(parserService).parseDocx(eq(file1));
        verify(parserService).parseDocx(eq(file2));
        verify(parserService).parseDocx(eq(file3));

        // 3. ZERO files should have been stored to disk or saved to DB
        verify(storageService, never()).storeDocument(any(), any());
        verify(importRepository, never()).save(any());
        verify(sourceDocumentRepository, never()).save(any());
        verify(questionRepository, never()).saveAll(any());
    }
}

