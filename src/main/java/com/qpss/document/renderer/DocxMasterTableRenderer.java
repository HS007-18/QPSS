package com.qpss.document.renderer;

import com.qpss.entity.Question;

import com.qpss.entity.GeneratedPaper;
import com.qpss.entity.PaperQuestion;
import com.qpss.repository.QuestionRepository;
import com.qpss.entity.Subject;
import com.qpss.document.model.HeaderMetadata;
import com.qpss.util.LayoutConstants;
import com.qpss.service.DocumentMetadataService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.List;

@Component
public class DocxMasterTableRenderer {

    private static final Logger log = LoggerFactory.getLogger(DocxMasterTableRenderer.class);

    private final QuestionRepository questionRepository;
    private final DocumentMetadataService documentMetadataService;

    // Delegate renderers
    private final HeaderRenderer headerRenderer = new HeaderRenderer();
    private final CourseOutcomesRenderer courseOutcomesRenderer = new CourseOutcomesRenderer();
    private final DurationInstructionsRenderer durationInstructionsRenderer = new DurationInstructionsRenderer();
    private final PartARenderer partARenderer;
    private final PartBRenderer partBRenderer;
    private final TableOfSpecificationRenderer tableOfSpecificationRenderer;
    private final SignaturesRenderer signaturesRenderer = new SignaturesRenderer();

    public DocxMasterTableRenderer(QuestionRepository questionRepository,
                                   DocumentMetadataService documentMetadataService) {
        this.questionRepository = questionRepository;
        this.documentMetadataService = documentMetadataService;
        this.partARenderer = new PartARenderer(questionRepository);
        this.partBRenderer = new PartBRenderer(questionRepository);
        this.tableOfSpecificationRenderer = new TableOfSpecificationRenderer(questionRepository);
    }

    public void renderMasterTable(XWPFDocument document, GeneratedPaper paper, Subject subject,
            List<PaperQuestion> sectionA, List<PaperQuestion> sectionB) {
        HeaderMetadata metadata = extractMetadata(sectionA, sectionB);

        // Set A4 portrait page size with narrow margins
        setPageSizePortraitA4(document);

        // Create one single master table with 5 columns
        XWPFTable table = document.createTable(1, 5);
        table.setWidth("100%");

        // Reduced cell margins to fit contents better
        table.setCellMargins(40, 80, 40, 80);

        // Set fixed column widths
        setColumnWidths(table, LayoutConstants.MASTER_TABLE_COLUMN_WIDTHS);

        int rowIndex = 0;

        // --- 1. HEADER SECTION ---
        rowIndex = headerRenderer.renderHeaderBlock(table, rowIndex, paper, subject, metadata);

        // --- 2. COURSE OUTCOMES SECTION ---
        rowIndex = courseOutcomesRenderer.renderCourseOutcomesBlock(table, rowIndex, paper, metadata);

        // --- 3. DURATION & MAX MARKS BAR ---
        rowIndex = durationInstructionsRenderer.renderDurationAndInstructionsBlock(table, rowIndex, paper);

        // --- 4. PART A SECTION ---
        rowIndex = partARenderer.renderPartA(table, rowIndex, sectionA);

        // --- 5. PART B SECTION ---
        rowIndex = partBRenderer.renderPartB(table, rowIndex, sectionB);

        // Remove borders from the master table (only ToS will have borders)
        table.removeBorders();

        // --- 6. TABLE OF SPECIFICATION (New Table) ---
        tableOfSpecificationRenderer.renderTableOfSpecification(document, sectionA, sectionB);

        // --- 7. SIGNATURES & AUDIT SECTION (New Table) ---
        signaturesRenderer.renderSignaturesBlock(document, paper);
    }

    private void setPageSizePortraitA4(XWPFDocument document) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTBody body = document.getDocument().getBody();
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr sectPr = body.isSetSectPr() ? body.getSectPr()
                : body.addNewSectPr();
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz pgSz = sectPr.isSetPgSz() ? sectPr.getPgSz()
                : sectPr.addNewPgSz();
        pgSz.setW(java.math.BigInteger.valueOf(11906));
        pgSz.setH(java.math.BigInteger.valueOf(16838));
        pgSz.setOrient(org.openxmlformats.schemas.wordprocessingml.x2006.main.STPageOrientation.PORTRAIT);
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar pgMar = sectPr.isSetPgMar() ? sectPr.getPgMar()
                : sectPr.addNewPgMar();
        pgMar.setTop(java.math.BigInteger.valueOf(LayoutConstants.PAGE_MARGIN_TOP));
        pgMar.setBottom(java.math.BigInteger.valueOf(LayoutConstants.PAGE_MARGIN_BOTTOM));
        pgMar.setLeft(java.math.BigInteger.valueOf(LayoutConstants.PAGE_MARGIN_LEFT));
        pgMar.setRight(java.math.BigInteger.valueOf(LayoutConstants.PAGE_MARGIN_RIGHT));
    }

    private void setColumnWidths(XWPFTable table, String[] widths) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl ctTbl = table.getCTTbl();
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid grid = ctTbl.getTblGrid() != null
                ? ctTbl.getTblGrid()
                : ctTbl.addNewTblGrid();
        while (grid.sizeOfGridColArray() > 0) {
            grid.removeGridCol(0);
        }
        for (String w : widths) {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol col = grid.addNewGridCol();
            col.setW(java.math.BigInteger.valueOf(Long.parseLong(w)));
        }
    }

    private HeaderMetadata extractMetadata(List<PaperQuestion> sectionA, List<PaperQuestion> sectionB) {
        Long sourceDocId = null;
        if (!sectionA.isEmpty()) {
            com.qpss.entity.Question q = questionRepository.findById(sectionA.get(0).getQuestionId()).orElse(null);
            if (q != null)
                sourceDocId = q.getSourceDocumentId();
        } else if (!sectionB.isEmpty()) {
            com.qpss.entity.Question q = questionRepository.findById(sectionB.get(0).getQuestionId()).orElse(null);
            if (q != null)
                sourceDocId = q.getSourceDocumentId();
        }

        if (sourceDocId != null) {
            return documentMetadataService.extractMetadata(sourceDocId);
        }
        return new HeaderMetadata();
    }
}