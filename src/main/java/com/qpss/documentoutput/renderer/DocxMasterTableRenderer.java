package com.qpss.documentoutput.renderer;

import com.qpss.backend.paper.GeneratedPaper;
import com.qpss.backend.paper.PaperQuestion;
import com.qpss.backend.questionbank.Question;
import com.qpss.backend.questionbank.QuestionRepository;
import com.qpss.backend.questionbank.SourceDocument;
import com.qpss.backend.questionbank.SourceDocumentRepository;
import com.qpss.backend.questionbank.SourceDocumentStorageService;
import com.qpss.backend.subject.Subject;
import com.qpss.documentextraction.extractor.HeaderMetadataExtractor;
import com.qpss.documentextraction.model.HeaderMetadata;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.*;

@Component
@RequiredArgsConstructor
public class DocxMasterTableRenderer {

    private static final Logger log = LoggerFactory.getLogger(DocxMasterTableRenderer.class);

    // Genuine institutional constants (Section 3 of requirements)
    private static final String INSTITUTION_NAME = "KANGEYAM INSTITUTE OF TECHNOLOGY";
    private static final String INSTITUTION_TAGLINE = "(An Autonomous Institution)";
    private static final int MAXIMUM_MARKS = 100;

    private final QuestionRepository questionRepository;
    private final SourceDocumentRepository sourceDocumentRepository;
    private final SourceDocumentStorageService storageService;
    private final HeaderMetadataExtractor metadataExtractor = new HeaderMetadataExtractor();

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

        // Set fixed column widths (in twips: 1 inch = 1440 twips, A4 usable ~9900 twips
        // with narrow margins)
        // Col 0: Q.No (900), Col 1: Question (6600), Col 2: M (600), Col 3: RBT (900),
        // Col 4: CO (900)
        setColumnWidths(table, new int[] { 900, 6600, 600, 900, 900 });

        int rowIndex = 0;

        // --- 1. HEADER SECTION ---
        rowIndex = renderHeaderBlock(table, rowIndex, paper, subject, metadata);

        // --- 2. COURSE OUTCOMES SECTION ---
        rowIndex = renderCourseOutcomesBlock(table, rowIndex, paper, metadata);

        // --- 3. DURATION & MAX MARKS BAR ---
        rowIndex = renderDurationAndInstructionsBlock(table, rowIndex, paper);

        // --- 4. PART A SECTION ---
        rowIndex = renderPartA(table, rowIndex, sectionA);

        // --- 5. PART B SECTION ---
        rowIndex = renderPartB(table, rowIndex, sectionB);

        // Apply borders to EVERYTHING in the master table as requested
        applyTableBorders(table);

        // --- 6. TABLE OF SPECIFICATION (New Table) ---
        renderTableOfSpecification(document, sectionA, sectionB);

        // --- 7. SIGNATURES & AUDIT SECTION (New Table) ---
        renderSignaturesBlock(document, paper);
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
        pgMar.setTop(java.math.BigInteger.valueOf(720));
        pgMar.setBottom(java.math.BigInteger.valueOf(720));
        pgMar.setLeft(java.math.BigInteger.valueOf(720));
        pgMar.setRight(java.math.BigInteger.valueOf(720));
    }

    private void setColumnWidths(XWPFTable table, int[] widths) {
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl ctTbl = table.getCTTbl();
        org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGrid grid = ctTbl.getTblGrid() != null
                ? ctTbl.getTblGrid()
                : ctTbl.addNewTblGrid();
        while (grid.sizeOfGridColArray() > 0) {
            grid.removeGridCol(0);
        }
        for (int w : widths) {
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblGridCol col = grid.addNewGridCol();
            col.setW(java.math.BigInteger.valueOf(w));
        }
    }

    private int renderHeaderBlock(XWPFTable table, int rIdx, GeneratedPaper paper, Subject subject,
            HeaderMetadata metadata) {
        // Row 0: QP Code (Left) and Reg No (Right)
        XWPFTableRow r0 = getOrCreateRow(table, rIdx++);
        mergeCells(r0, 0, 1);
        setCellText(r0.getCell(0), "QUESTION PAPER CODE: __________", ParagraphAlignment.LEFT, false, 11);
        mergeCells(r0, 2, 4);
        setCellText(r0.getCell(2), "REG.\u00A0NO.:\u00A0__________", ParagraphAlignment.RIGHT, false, 11);

        // Row 1: Institution name (constant) + tagline (constant)
        XWPFTableRow r1 = getOrCreateRow(table, rIdx++);
        mergeCells(r1, 0, 4);
        String institution = metadata.getInstitutionName() != null ? metadata.getInstitutionName()
                : INSTITUTION_NAME;
        String tagline = metadata.getTagline() != null ? metadata.getTagline() : INSTITUTION_TAGLINE;
        setCellText(r1.getCell(0), institution + " " + tagline, ParagraphAlignment.CENTER, true, 11);

        // Row 2: Exam title (from metadata or mapped from ExamType)
        XWPFTableRow r2 = getOrCreateRow(table, rIdx++);
        mergeCells(r2, 0, 4);
        String examTitle = resolveExamTitle(paper, metadata);
        setCellText(r2.getCell(0), "B.E. / B.Tech. " + examTitle, ParagraphAlignment.CENTER, true, 11);

        // Row 3: Date/Session (Left) and Academic Year (Right)
        XWPFTableRow r3 = getOrCreateRow(table, rIdx++);
        mergeCells(r3, 0, 1);
        setCellText(r3.getCell(0), "Date / Session: __________", ParagraphAlignment.LEFT, false, 11);
        mergeCells(r3, 2, 4);
        setCellText(r3.getCell(2), "Academic Year: __________", ParagraphAlignment.RIGHT, false, 11);

        // Row 4: Semester (from source) - Department (from source)
        XWPFTableRow r4 = getOrCreateRow(table, rIdx++);
        mergeCells(r4, 0, 4);
        String semStr = metadata.getSemester() != null ? metadata.getSemester() : "";
        String deptStr = metadata.getDepartment() != null ? metadata.getDepartment() : "";
        String semDept = !semStr.isEmpty() && !deptStr.isEmpty() ? semStr + " - " + deptStr
                : !semStr.isEmpty() ? semStr : deptStr;
        setCellText(r4.getCell(0), semDept, ParagraphAlignment.CENTER, true, 11);

        // Row 5: Subject code and title (from source, fallback to Subject entity name)
        XWPFTableRow r5 = getOrCreateRow(table, rIdx++);
        mergeCells(r5, 0, 4);
        String subjCodeTitle = metadata.getSubjectCodeTitle() != null ? metadata.getSubjectCodeTitle()
                : (subject.getName() != null ? subject.getName().toUpperCase() : "");
        setCellText(r5.getCell(0), subjCodeTitle, ParagraphAlignment.CENTER, true, 11);

        // Row 5.5: Common-to (from source, skip if absent)
        if (metadata.getCommonTo() != null && !metadata.getCommonTo().isBlank()) {
            XWPFTableRow rCommon = getOrCreateRow(table, rIdx++);
            mergeCells(rCommon, 0, 4);
            setCellText(rCommon.getCell(0), metadata.getCommonTo(), ParagraphAlignment.CENTER, false, 11);
        }

        // Row 5.6: Note (from source, skip if absent)
        if (metadata.getNotes() != null && !metadata.getNotes().isBlank()) {
            XWPFTableRow rNote = getOrCreateRow(table, rIdx++);
            mergeCells(rNote, 0, 4);
            setCellText(rNote.getCell(0), metadata.getNotes(), ParagraphAlignment.CENTER, false, 11);
        }

        // Row 6: Regulation (from source, no hardcoded fallback)
        XWPFTableRow r6 = getOrCreateRow(table, rIdx++);
        mergeCells(r6, 0, 4);
        String regStr = metadata.getRegulation() != null ? metadata.getRegulation() : "";
        setCellText(r6.getCell(0), regStr, ParagraphAlignment.CENTER, true, 11);

        return rIdx;
    }

    private int renderCourseOutcomesBlock(XWPFTable table, int rIdx, GeneratedPaper paper, HeaderMetadata metadata) {
        // Render ALL COs from source document without hardcoded filtering.
        // The source document already contains the applicable COs for this exam.
        List<HeaderMetadata.CourseOutcome> cos = metadata.getCourseOutcomes() != null ? metadata.getCourseOutcomes()
                : List.of();

        if (cos.isEmpty()) {
            return rIdx; // No COs in source — skip entirely, do not fabricate
        }

        XWPFTableRow rHead = getOrCreateRow(table, rIdx++);
        mergeCells(rHead, 0, 4);
        setCellText(rHead.getCell(0), "COURSE OUTCOMES (COs): Students will be able to", ParagraphAlignment.LEFT, true,
                10);

        for (HeaderMetadata.CourseOutcome co : cos) {
            XWPFTableRow rCo = getOrCreateRow(table, rIdx++);
            mergeCells(rCo, 1, 4);
            setCellText(rCo.getCell(0), co.getCode() + ":", ParagraphAlignment.LEFT, true, 10);
            setCellText(rCo.getCell(1), co.getDescription(), ParagraphAlignment.LEFT, false, 10);
        }

        return rIdx;
    }

    private int renderDurationAndInstructionsBlock(XWPFTable table, int rIdx, GeneratedPaper paper) {
        String durationStr = paper.getDuration() != null ? paper.getDuration() : "Three Hours";
        XWPFTableRow rDur = getOrCreateRow(table, rIdx++);
        mergeCells(rDur, 0, 1);
        setCellText(rDur.getCell(0), "Duration: " + durationStr, ParagraphAlignment.LEFT, true, 10);
        mergeCells(rDur, 2, 4);
        setCellText(rDur.getCell(2), "Maximum:\u00A0" + MAXIMUM_MARKS + "\u00A0Marks", ParagraphAlignment.RIGHT, true, 10);
        return rIdx;
    }

    private int renderPartA(XWPFTable table, int rIdx, List<PaperQuestion> sectionA) {
        if (sectionA.isEmpty())
            return rIdx;

        // Dynamically determine Part A marks from actual questions
        int qCount = sectionA.size();
        int perQuestionMarks = 2; // default
        Question firstA = questionRepository.findById(sectionA.get(0).getQuestionId()).orElse(null);
        if (firstA != null && firstA.getMarks() != null) {
            perQuestionMarks = firstA.getMarks();
        }

        XWPFTableRow rPartA = getOrCreateRow(table, rIdx++);
        mergeCells(rPartA, 0, 4);
        setCellText(rPartA.getCell(0), "PART - A (" + qCount + " x " + perQuestionMarks + " = " + (qCount * perQuestionMarks) + " Marks)",
                ParagraphAlignment.CENTER, true, 10);

        XWPFTableRow rInst = getOrCreateRow(table, rIdx++);
        mergeCells(rInst, 0, 4);
        setCellText(rInst.getCell(0), "Answer ALL Questions.", ParagraphAlignment.CENTER, true, 10);

        XWPFTableRow rHeader = getOrCreateRow(table, rIdx++);
        setCellText(rHeader.getCell(0), "", ParagraphAlignment.CENTER, true, 10);
        setCellText(rHeader.getCell(1), "", ParagraphAlignment.CENTER, true, 10);
        setCellText(rHeader.getCell(2), "M", ParagraphAlignment.CENTER, true, 10);
        setCellText(rHeader.getCell(3), "RBT", ParagraphAlignment.CENTER, true, 10);
        setCellText(rHeader.getCell(4), "CO", ParagraphAlignment.CENTER, true, 10);

        for (PaperQuestion pq : sectionA) {
            Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
            if (q == null)
                continue;

            XWPFTableRow r = getOrCreateRow(table, rIdx++);
            setCellText(r.getCell(0), String.valueOf(pq.getQuestionNumber()), ParagraphAlignment.CENTER, false, 10);
            // Left alignment for questions
            renderQuestionCell(r.getCell(1), q, ParagraphAlignment.LEFT);
            setCellText(r.getCell(2), marksLabel(q), ParagraphAlignment.CENTER, false, 10);
            setCellText(r.getCell(3), rbtLabel(q), ParagraphAlignment.CENTER, false, 10);
            setCellText(r.getCell(4), coLabel(q), ParagraphAlignment.CENTER, false, 10);
        }

        return rIdx;
    }

    private int renderPartB(XWPFTable table, int rIdx, List<PaperQuestion> sectionB) {
        if (sectionB.isEmpty())
            return rIdx;

        // Dynamically determine Part B marks from actual questions — supports 16-mark and 20-mark
        int partBMarks = 16;
        Question sampleQ = questionRepository.findById(sectionB.get(0).getQuestionId()).orElse(null);
        if (sampleQ != null && sampleQ.getMarks() != null) {
            partBMarks = sampleQ.getMarks();
        }
        int pairsCount = sectionB.size() / 2;

        XWPFTableRow rPartB = getOrCreateRow(table, rIdx++);
        mergeCells(rPartB, 0, 4);
        setCellText(rPartB.getCell(0),
                "PART - B (" + pairsCount + " x " + partBMarks + " = " + (pairsCount * partBMarks) + " Marks)",
                ParagraphAlignment.CENTER, true, 10);

        Integer currentQNum = null;
        for (PaperQuestion pq : sectionB) {
            Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);

            if (currentQNum != null && currentQNum.equals(pq.getQuestionNumber())) {
                XWPFTableRow orRow = getOrCreateRow(table, rIdx++);
                mergeCells(orRow, 0, 4);
                setCellText(orRow.getCell(0), "OR", ParagraphAlignment.CENTER, true, 10);
            } else {
                currentQNum = pq.getQuestionNumber();
            }

            XWPFTableRow r = getOrCreateRow(table, rIdx++);
            setCellText(r.getCell(0),
                    pq.getQuestionNumber() + ".\u00A0(" + (pq.getChoiceLabel() != null ? pq.getChoiceLabel() : "a") + ")",
                    ParagraphAlignment.CENTER, false, 10);
            // Left alignment for questions
            renderQuestionCell(r.getCell(1), q, ParagraphAlignment.LEFT);
            setCellText(r.getCell(2), q != null ? marksLabel(q) : "", ParagraphAlignment.CENTER, false, 10);
            setCellText(r.getCell(3), q != null ? rbtLabel(q) : "", ParagraphAlignment.CENTER, false, 10);
            setCellText(r.getCell(4), q != null ? coLabel(q) : "", ParagraphAlignment.CENTER, false, 10);
        }

        return rIdx;
    }

    private void renderTableOfSpecification(XWPFDocument document, List<PaperQuestion> sectionA, List<PaperQuestion> sectionB) {
        document.createParagraph().createRun().addBreak();
        
        XWPFTable table = document.createTable(1, 5);
        table.setWidth("100%");
        table.setCellMargins(40, 80, 40, 80);
        setColumnWidths(table, new int[] { 3000, 1500, 1500, 1500, 2400 });

        int rIdx = 0;
        XWPFTableRow rTitle = getOrCreateRow(table, rIdx++);
        mergeCells(rTitle, 0, 4);
        setCellText(rTitle.getCell(0), "TABLE OF SPECIFICATION", ParagraphAlignment.CENTER, true, 10);

        XWPFTableRow rH1 = getOrCreateRow(table, rIdx++);
        mergeCells(rH1, 0, 1);
        setCellText(rH1.getCell(0), "Revised Bloom’s Taxonomy (RBT)", ParagraphAlignment.CENTER, false, 10);
        mergeCells(rH1, 2, 3);
        setCellText(rH1.getCell(2), "Marks Distribution", ParagraphAlignment.CENTER, false, 10);
        setCellText(rH1.getCell(4), "Total Marks", ParagraphAlignment.CENTER, false, 10);

        XWPFTableRow rH2 = getOrCreateRow(table, rIdx++);
        mergeCells(rH2, 0, 1);
        setCellText(rH2.getCell(0), "Cognitive Levels", ParagraphAlignment.CENTER, false, 10);
        setCellText(rH2.getCell(2), "Part - A", ParagraphAlignment.CENTER, false, 10);
        setCellText(rH2.getCell(3), "Part - B", ParagraphAlignment.CENTER, false, 10);
        setCellText(rH2.getCell(4), "", ParagraphAlignment.CENTER, false, 10);

        Map<String, Integer> marksA = calculateRbtMarks(sectionA);
        Map<String, Integer> marksB = calculateRbtMarks(sectionB);

        String[][] rbtLevels = {
                { "Remember", "R" },
                { "Understand", "U" },
                { "Apply", "Ap" },
                { "Analyze", "Az" },
                { "Evaluate", "E" },
                { "Create", "C" }
        };

        int totalA = 0, totalB = 0;

        for (String[] level : rbtLevels) {
            String name = level[0];
            String code = level[1];
            int mA = marksA.getOrDefault(code, 0);
            int mB = marksB.getOrDefault(code, 0);
            int total = mA + mB;
            totalA += mA;
            totalB += mB;

            XWPFTableRow rLevel = getOrCreateRow(table, rIdx++);
            setCellText(rLevel.getCell(0), name, ParagraphAlignment.CENTER, false, 10);
            setCellText(rLevel.getCell(1), code, ParagraphAlignment.CENTER, false, 10);
            setCellText(rLevel.getCell(2), mA > 0 ? String.valueOf(mA) : "", ParagraphAlignment.CENTER, false, 10);
            setCellText(rLevel.getCell(3), mB > 0 ? String.valueOf(mB) : "", ParagraphAlignment.CENTER, false, 10);
            setCellText(rLevel.getCell(4), total > 0 ? String.valueOf(total) : "", ParagraphAlignment.CENTER, false, 10);
        }

        XWPFTableRow rTot = getOrCreateRow(table, rIdx++);
        mergeCells(rTot, 0, 1);
        setCellText(rTot.getCell(0), "Total Marks", ParagraphAlignment.RIGHT, false, 10);
        setCellText(rTot.getCell(2), String.valueOf(totalA), ParagraphAlignment.CENTER, false, 10);
        setCellText(rTot.getCell(3), String.valueOf(totalB), ParagraphAlignment.CENTER, false, 10);
        setCellText(rTot.getCell(4), String.valueOf(totalA + totalB), ParagraphAlignment.CENTER, false, 10);

        applyTableBorders(table);
    }

    private void renderSignaturesBlock(XWPFDocument document, GeneratedPaper paper) {
        document.createParagraph().createRun().addBreak();
        
        XWPFTable table = document.createTable(1, 3);
        table.setWidth("100%");
        table.setCellMargins(0, 0, 0, 0);
        table.removeBorders();

        int rIdx = 0;
        boolean isEse = paper.getExamType() != null
                && (paper.getExamType().equalsIgnoreCase("ESE") || paper.getExamType().equalsIgnoreCase("FAT"));

        if (isEse) {
            XWPFTableRow r1 = getOrCreateRowSignatures(table, rIdx++);
            setCellText(r1.getCell(2), "Audited by", ParagraphAlignment.CENTER, false, 11);

            XWPFTableRow rSpace = getOrCreateRowSignatures(table, rIdx++);
            setCellText(rSpace.getCell(0), "\n\n", ParagraphAlignment.CENTER, false, 11);

            XWPFTableRow r2 = getOrCreateRowSignatures(table, rIdx++);
            setCellText(r2.getCell(2), "………………………...\nScrutiny Member", ParagraphAlignment.CENTER, false, 11);
        } else {
            XWPFTableRow r1 = getOrCreateRowSignatures(table, rIdx++);
            setCellText(r1.getCell(0), "Verified by", ParagraphAlignment.CENTER, false, 11);
            setCellText(r1.getCell(1), "Approved by", ParagraphAlignment.CENTER, false, 11);
            setCellText(r1.getCell(2), "Audited by", ParagraphAlignment.CENTER, false, 11);

            XWPFTableRow rSpace = getOrCreateRowSignatures(table, rIdx++);
            setCellText(rSpace.getCell(0), "\n\n", ParagraphAlignment.CENTER, false, 11);

            XWPFTableRow r2 = getOrCreateRowSignatures(table, rIdx++);
            setCellText(r2.getCell(0), "………………………...\nCourse Instructor /\nCoordinator", ParagraphAlignment.CENTER, false, 11);
            setCellText(r2.getCell(1), "………………..…………\nHead of the\nDepartment", ParagraphAlignment.CENTER, false, 11);
            setCellText(r2.getCell(2), "………………..………..\nIQAC\nCoordinator", ParagraphAlignment.CENTER, false, 11);
        }
        
        XWPFParagraph endPara = document.createParagraph();
        endPara.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun endRun = endPara.createRun();
        endRun.setText("***");
        endRun.setFontFamily("Times New Roman");
        endRun.setFontSize(11);
    }
    
    private XWPFTableRow getOrCreateRowSignatures(XWPFTable table, int index) {
        while (table.getRows().size() <= index) {
            table.createRow();
        }
        XWPFTableRow row = table.getRow(index);
        while (row.getTableCells().size() < 3) {
            row.createCell();
        }
        return row;
    }

    // Helper Utilities
    private XWPFTableRow getOrCreateRow(XWPFTable table, int index) {
        while (table.getRows().size() <= index) {
            table.createRow();
        }
        XWPFTableRow row = table.getRow(index);
        while (row.getTableCells().size() < 5) {
            row.createCell();
        }
        return row;
    }

    private void mergeCells(XWPFTableRow row, int fromCol, int toCol) {
        for (int cellIndex = fromCol; cellIndex <= toCol; cellIndex++) {
            XWPFTableCell cell = row.getCell(cellIndex);
            if (cellIndex == fromCol) {
                cell.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.RESTART);
            } else {
                cell.getCTTc().addNewTcPr().addNewHMerge().setVal(STMerge.CONTINUE);
            }
        }
    }

    private void setCellText(XWPFTableCell cell, String text, ParagraphAlignment align, boolean bold, int fontSize) {
        if (cell.getParagraphs().isEmpty()) {
            cell.addParagraph();
        }
        XWPFParagraph p = cell.getParagraphs().get(0);
        p.setAlignment(align);

        // Remove existing runs
        while (p.getRuns().size() > 0) {
            p.removeRun(0);
        }

        if (text != null && !text.isEmpty()) {
            String[] lines = text.split("\n");
            for (int i = 0; i < lines.length; i++) {
                XWPFRun r = p.createRun();
                r.setText(lines[i]);
                r.setBold(bold);
                r.setFontFamily("Times New Roman");
                r.setFontSize(fontSize);
                if (i < lines.length - 1) {
                    r.addBreak();
                }
            }
        }
    }

    private void applyTableBorders(XWPFTable table) {
        table.setTopBorder(XWPFTable.XWPFBorderType.SINGLE, 0, 0, "000000");
        table.setBottomBorder(XWPFTable.XWPFBorderType.SINGLE, 0, 0, "000000");
        table.setLeftBorder(XWPFTable.XWPFBorderType.SINGLE, 0, 0, "000000");
        table.setRightBorder(XWPFTable.XWPFBorderType.SINGLE, 0, 0, "000000");
        table.setInsideHBorder(XWPFTable.XWPFBorderType.SINGLE, 0, 0, "000000");
        table.setInsideVBorder(XWPFTable.XWPFBorderType.SINGLE, 0, 0, "000000");
    }

    private void renderQuestionCell(XWPFTableCell cell, Question q, ParagraphAlignment alignment) {
        if (q != null && q.getStructuredContent() != null && !q.getStructuredContent().isEmpty()) {
            AstToWordRenderer.setCellAst(cell, q.getStructuredContent());
        } else {
            HtmlToWordRenderer.setCellHtml(cell, q != null ? q.getQuestionContent() : "");
        }

        // Enforce alignment on the cell's paragraphs
        for (XWPFParagraph p : cell.getParagraphs()) {
            p.setAlignment(alignment);
        }
    }

    private Map<String, Integer> calculateRbtMarks(List<PaperQuestion> pqs) {
        Map<String, Integer> res = new HashMap<>();
        for (PaperQuestion pq : pqs) {
            Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
            if (q != null && q.getRbt() != null && q.getMarks() != null) {
                String code = q.getRbt().trim().toUpperCase();
                res.put(code, res.getOrDefault(code, 0) + q.getMarks());
            }
        }
        return res;
    }

    private String marksLabel(Question q) {
        if (q.getMarksSplit() != null && !q.getMarksSplit().isBlank()) {
            return q.getMarksSplit();
        }
        return q.getMarks() != null ? String.valueOf(q.getMarks()) : "";
    }

    private String coLabel(Question q) {
        if (q.getCo() == null || q.getCo().isBlank()) {
            return "";
        }
        String co = q.getCo().trim();
        return co.toUpperCase().startsWith("CO") ? co : "CO" + co;
    }

    private String rbtLabel(Question q) {
        if (q.getRbt() == null || q.getRbt().isBlank()) {
            return "";
        }
        return q.getRbt().trim().toUpperCase();
    }

    /**
     * Resolves the examination title from metadata or ExamType.
     * Priority: 1) metadata.examTitle from source document
     *           2) Mapped from GeneratedPaper.examType
     */
    private String resolveExamTitle(GeneratedPaper paper, HeaderMetadata metadata) {
        // Use source document exam title if available
        if (metadata.getExamTitle() != null && !metadata.getExamTitle().isBlank()) {
            return metadata.getExamTitle();
        }
        // Map from exam type enum
        if (paper.getExamType() != null) {
            String type = paper.getExamType().toUpperCase();
            if (type.equals("INTERNAL_1")) {
                return "CONTINUOUS INTERNAL ASSESSMENT EXAMINATIONS - I";
            } else if (type.equals("INTERNAL_2")) {
                return "CONTINUOUS INTERNAL ASSESSMENT EXAMINATIONS - II";
            } else if (type.equals("SEMESTER")) {
                String session = metadata.getExamSession();
                if (session != null && !session.isBlank()) {
                    return "END SEMESTER EXAMINATIONS, " + session;
                }
                return "END SEMESTER EXAMINATIONS";
            }
        }
        return "EXAMINATIONS";
    }

    private HeaderMetadata extractMetadata(List<PaperQuestion> sectionA, List<PaperQuestion> sectionB) {
        Long sourceDocId = null;
        if (!sectionA.isEmpty()) {
            Question q = questionRepository.findById(sectionA.get(0).getQuestionId()).orElse(null);
            if (q != null)
                sourceDocId = q.getSourceDocumentId();
        } else if (!sectionB.isEmpty()) {
            Question q = questionRepository.findById(sectionB.get(0).getQuestionId()).orElse(null);
            if (q != null)
                sourceDocId = q.getSourceDocumentId();
        }

        if (sourceDocId != null) {
            SourceDocument doc = sourceDocumentRepository.findById(sourceDocId).orElse(null);
            if (doc != null) {
                try {
                    byte[] fileBytes = storageService.loadDocument(doc.getStoredFileName());
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes);
                            XWPFDocument docx = new XWPFDocument(bis)) {
                        return metadataExtractor.extract(docx);
                    }
                } catch (Exception e) {
                    log.warn("Header metadata extraction failed", e);
                }
            }
        }
        return new HeaderMetadata();
    }
}
