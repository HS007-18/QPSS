package com.qpss.controller;

import com.qpss.entity.GeneratedPaper;
import com.qpss.service.DocxRendererService;
import com.qpss.service.ExamConfigService;
import com.qpss.service.PaperGenerationService;
import com.qpss.service.PartAOnlyGenerationService;
import com.qpss.service.SubjectService;
import com.qpss.util.QuestionContentSanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GenerationControllerTest {

    @Mock
    private PaperGenerationService generationService;

    @Mock
    private SubjectService subjectService;

    @Mock
    private DocxRendererService docxRendererService;

    @Mock
    private ExamConfigService examConfigService;

    @Mock
    private QuestionContentSanitizer contentSanitizer;

    @Mock
    private PartAOnlyGenerationService partAOnlyGenService;

    @InjectMocks
    private GenerationController generationController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testExportDocx_PureDownloadWithoutStateMutation() {
        Long subjectId = 100L;
        Long paperId = 5L;

        GeneratedPaper paper = GeneratedPaper.builder()
                .id(paperId)
                .subjectId(subjectId)
                .batchId("batch-1")
                .setLabel("A")
                .build();

        byte[] fakeDocx = new byte[]{1, 2, 3, 4};

        when(generationService.getPaperByIdAndSubjectId(paperId, subjectId)).thenReturn(Optional.of(paper));
        when(docxRendererService.exportPaperToDocx(paper)).thenReturn(fakeDocx);

        ResponseEntity<byte[]> response = generationController.exportDocx(subjectId, paperId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(fakeDocx, response.getBody());

        ContentDisposition cd = response.getHeaders().getContentDisposition();
        assertTrue(cd.isAttachment());
        assertEquals("QuestionPaper_Set_A.docx", cd.getFilename());

        verify(docxRendererService).exportPaperToDocx(paper);
    }

    @Test
    void testGenerate_Success_ReturnsReviewView() {
        Long subjectId = 1L;
        com.qpss.entity.Subject subject = com.qpss.entity.Subject.builder().id(subjectId).name("Math").build();
        when(subjectService.findById(subjectId)).thenReturn(subject);

        com.qpss.service.PaperGenerationService.GenerationResult result =
                new com.qpss.service.PaperGenerationService.GenerationResult("batch-xyz", java.util.Collections.emptyList(), java.util.Collections.emptyList(), null, true);

        when(generationService.generate(eq("INTERNAL_1"), eq(subjectId), eq(1), eq("FORMAT_1"), eq("Three Hours")))
                .thenReturn(result);

        org.springframework.ui.ExtendedModelMap model = new org.springframework.ui.ExtendedModelMap();
        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirect = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = generationController.generate(
                subjectId, "INTERNAL_1", 1, "FORMAT_1", "Three Hours", java.util.Collections.emptyMap(), model, redirect
        );

        assertEquals("generation/review", view);
        assertEquals(subject, model.getAttribute("subject"));
        assertEquals(result, model.getAttribute("result"));
    }

    @Test
    void testGenerate_Shortage_RedirectsToSubject() {
        Long subjectId = 1L;
        com.qpss.entity.Subject subject = com.qpss.entity.Subject.builder().id(subjectId).name("Math").build();
        when(subjectService.findById(subjectId)).thenReturn(subject);

        com.qpss.domain.selection.SelectionEngine.SelectionShortage shortage =
                new com.qpss.domain.selection.SelectionEngine.SelectionShortage(1, 2, 1, 10, 5, 5);
        com.qpss.service.PaperGenerationService.GenerationResult result =
                new com.qpss.service.PaperGenerationService.GenerationResult("batch-xyz", java.util.Collections.emptyList(), java.util.List.of(shortage), null, false);

        when(generationService.generate(eq("INTERNAL_1"), eq(subjectId), eq(1), eq("FORMAT_1"), eq("Three Hours")))
                .thenReturn(result);

        org.springframework.ui.ExtendedModelMap model = new org.springframework.ui.ExtendedModelMap();
        org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap redirect = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        String view = generationController.generate(
                subjectId, "INTERNAL_1", 1, "FORMAT_1", "Three Hours", java.util.Collections.emptyMap(), model, redirect
        );

        assertEquals("redirect:/subjects/" + subjectId, view);
        assertNotNull(redirect.getFlashAttributes().get("shortages"));
    }

    @Test
    void testSwapQuestion_Success_ReturnsNewQuestionPayload() {
        Long subjectId = 1L;
        Long paperId = 5L;
        Long oldQId = 10L;

        com.qpss.entity.Question newQ = com.qpss.entity.Question.builder()
                .id(20L)
                .marks(2)
                .unit(1)
                .rbt("U")
                .questionContent("New Question Content")
                .build();

        when(generationService.swapQuestion(paperId, oldQId, subjectId)).thenReturn(newQ);
        when(contentSanitizer.sanitize(anyString())).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<java.util.Map<String, Object>> response = generationController.swapQuestion(subjectId, paperId, oldQId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("success", response.getBody().get("status"));
        assertEquals(20L, response.getBody().get("newId"));
    }
}
