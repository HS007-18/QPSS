package com.qpss.controller;

import com.qpss.entity.GeneratedPaper;
import com.qpss.service.DocxRendererService;
import com.qpss.service.ExamConfigService;
import com.qpss.service.PaperGenerationService;
import com.qpss.service.PartAOnlyGenerationService;
import com.qpss.service.SessionService;
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
    private SessionService sessionService;

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
        Long sessionId = 100L;
        Long paperId = 5L;

        GeneratedPaper paper = GeneratedPaper.builder()
                .id(paperId)
                .sessionId(sessionId)
                .setLabel("A")
                .build();

        byte[] fakeDocx = new byte[]{1, 2, 3, 4};

        when(generationService.getPaperByIdAndSessionId(paperId, sessionId)).thenReturn(Optional.of(paper));
        when(docxRendererService.exportPaperToDocx(paper)).thenReturn(fakeDocx);

        ResponseEntity<byte[]> response = generationController.exportDocx(sessionId, paperId);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(fakeDocx, response.getBody());

        ContentDisposition cd = response.getHeaders().getContentDisposition();
        assertTrue(cd.isAttachment());
        assertEquals("QuestionPaper_Set_A.docx", cd.getFilename());

        verify(docxRendererService).exportPaperToDocx(paper);
    }
}
