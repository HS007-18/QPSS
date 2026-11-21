package com.qpss.service;

import com.qpss.entity.GeneratedPaper;
import com.qpss.entity.Session;
import com.qpss.entity.SourceDocument;
import com.qpss.entity.Subject;
import com.qpss.repository.GeneratedPaperRepository;
import com.qpss.repository.PaperQuestionRepository;
import com.qpss.repository.QuestionBankImportRepository;
import com.qpss.repository.QuestionRepository;
import com.qpss.repository.SessionRepository;
import com.qpss.repository.SourceDocumentRepository;
import com.qpss.repository.SubjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SubjectServiceTest {

    @Mock
    private SubjectRepository repo;

    @Mock
    private SessionRepository sessionRepo;

    @Mock
    private QuestionRepository questionRepo;

    @Mock
    private GeneratedPaperRepository paperRepo;

    @Mock
    private PaperQuestionRepository paperQuestionRepo;

    @Mock
    private QuestionBankImportRepository importRepo;

    @Mock
    private SourceDocumentRepository sourceDocumentRepo;

    @Mock
    private SourceDocumentStorageService storageService;

    @InjectMocks
    private SubjectService subjectService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDelete_RemovesPhysicalFilesAndAllAssociatedEntities() {
        Long subjectId = 10L;
        Long sessionId = 100L;
        Long paperId = 50L;

        Session session = Session.builder().id(sessionId).subjectId(subjectId).build();
        when(sessionRepo.findBySubjectId(subjectId)).thenReturn(List.of(session));

        GeneratedPaper paper = GeneratedPaper.builder().id(paperId).subjectId(subjectId).sessionId(sessionId).build();
        when(paperRepo.findBySubjectId(subjectId)).thenReturn(List.of(paper));
        when(paperRepo.findBySessionIdIn(List.of(sessionId))).thenReturn(List.of(paper));

        SourceDocument doc1 = SourceDocument.builder()
                .id(1L)
                .storedFileName("stored-file-1.docx")
                .build();
        SourceDocument doc2 = SourceDocument.builder()
                .id(2L)
                .storedFileName("stored-file-2.docx")
                .build();

        when(sourceDocumentRepo.findByImportBatch_SubjectId(subjectId)).thenReturn(List.of(doc1, doc2));
        when(sourceDocumentRepo.findByImportBatch_SessionIdIn(List.of(sessionId))).thenReturn(Collections.emptyList());

        subjectService.delete(subjectId);

        // Verify physical files deleted from disk storage
        verify(storageService).deleteDocument("stored-file-1.docx");
        verify(storageService).deleteDocument("stored-file-2.docx");

        // Verify database cleanup sequence
        verify(paperQuestionRepo).deleteByPaperIdIn(List.of(paperId));
        verify(paperRepo).deleteBySubjectId(subjectId);
        verify(questionRepo).deleteBySubjectId(subjectId);
        verify(sourceDocumentRepo).deleteByImportBatch_SubjectId(subjectId);
        verify(importRepo).deleteBySubjectId(subjectId);
        verify(sessionRepo).deleteBySubjectId(subjectId);
        verify(repo).deleteById(subjectId);
    }

    @Test
    void testDelete_WhenNoSessions_StillDeletesQuestionsAndPhysicalFilesAndSubject() {
        Long subjectId = 20L;

        when(sessionRepo.findBySubjectId(subjectId)).thenReturn(Collections.emptyList());
        when(paperRepo.findBySubjectId(subjectId)).thenReturn(Collections.emptyList());

        SourceDocument doc = SourceDocument.builder()
                .id(5L)
                .storedFileName("isolated-file.docx")
                .build();

        when(sourceDocumentRepo.findByImportBatch_SubjectId(subjectId)).thenReturn(List.of(doc));

        subjectService.delete(subjectId);

        // Physical file deleted
        verify(storageService).deleteDocument("isolated-file.docx");

        // Questions and source documents removed
        verify(questionRepo).deleteBySubjectId(subjectId);
        verify(sourceDocumentRepo).deleteByImportBatch_SubjectId(subjectId);
        verify(importRepo).deleteBySubjectId(subjectId);
        verify(sessionRepo).deleteBySubjectId(subjectId);
        verify(repo).deleteById(subjectId);
    }

    @Test
    void testFindOrCreate_ExistingSubjectByCode() {
        Subject existing = Subject.builder().id(1L).code("CS101").name("Intro to CS").build();
        when(repo.findByCodeIgnoreCase("CS101")).thenReturn(Optional.of(existing));

        Subject result = subjectService.findOrCreate("CS101", "Intro to CS Updated");

        assertEquals(existing, result);
        verify(repo, never()).save(any());
    }

    @Test
    void testFindOrCreate_NewSubjectCreated() {
        when(repo.findByCodeIgnoreCase("CS102")).thenReturn(Optional.empty());
        Subject created = Subject.builder().id(2L).code("CS102").name("Data Structures").build();
        when(repo.save(any(Subject.class))).thenReturn(created);

        Subject result = subjectService.findOrCreate("cs102", "Data Structures");

        assertNotNull(result);
        assertEquals("CS102", result.getCode());
        verify(repo).save(any(Subject.class));
    }
}
