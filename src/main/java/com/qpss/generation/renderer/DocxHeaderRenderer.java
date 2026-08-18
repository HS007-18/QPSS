package com.qpss.generation.renderer;

import com.qpss.generation.model.GeneratedPaper;
import com.qpss.generation.model.PaperQuestion;
import com.qpss.questionbank.model.HeaderMetadata;
import com.qpss.questionbank.model.Question;
import com.qpss.questionbank.model.SourceDocument;
import com.qpss.questionbank.parser.HeaderMetadataExtractor;
import com.qpss.questionbank.repository.QuestionRepository;
import com.qpss.questionbank.repository.SourceDocumentRepository;
import com.qpss.questionbank.service.SourceDocumentStorageService;
import com.qpss.subject.model.Subject;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class DocxHeaderRenderer {

    private static final Logger log = LoggerFactory.getLogger(DocxHeaderRenderer.class);

    private final QuestionRepository questionRepository;
    private final SourceDocumentRepository sourceDocumentRepository;
    private final SourceDocumentStorageService storageService;
    private final HeaderMetadataExtractor metadataExtractor = new HeaderMetadataExtractor();

    public void render(XWPFDocument document, GeneratedPaper paper, Subject subject, List<PaperQuestion> sectionA, List<PaperQuestion> sectionB) {
        Long sourceDocId = null;
        if (sectionA != null && !sectionA.isEmpty()) {
            Question q = questionRepository.findById(sectionA.get(0).getQuestionId()).orElse(null);
            if (q != null) sourceDocId = q.getSourceDocumentId();
        } else if (sectionB != null && !sectionB.isEmpty()) {
            Question q = questionRepository.findById(sectionB.get(0).getQuestionId()).orElse(null);
            if (q != null) sourceDocId = q.getSourceDocumentId();
        }

        HeaderMetadata metadata = new HeaderMetadata();
        if (sourceDocId != null) {
            SourceDocument sourceDoc = sourceDocumentRepository.findById(sourceDocId).orElse(null);
            if (sourceDoc != null) {
                try {
                    byte[] fileBytes = storageService.loadDocument(sourceDoc.getStoredFileName());
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes); XWPFDocument sourceDocx = new XWPFDocument(bis)) {
                        metadata = metadataExtractor.extract(sourceDocx);
                    }
                } catch (Exception e) {
                    log.warn("Extraction failed", e);
                }
            }
        }
        renderStandardizedHeader(document, paper, subject, metadata);
    }

    private void renderStandardizedHeader(XWPFDocument document, GeneratedPaper paper, Subject subject, HeaderMetadata metadata) {
        XWPFTable topTable = document.createTable(1, 3);
        removeTableBorders(topTable);
        topTable.setWidth("100%");
        setCellText(topTable.getRow(0).getCell(0), "QP CODE: _________________", ParagraphAlignment.LEFT, false, 11);
        setCellText(topTable.getRow(0).getCell(1), "Date: _____________", ParagraphAlignment.CENTER, false, 11);
        setCellText(topTable.getRow(0).getCell(2), "Register No.: _______________", ParagraphAlignment.RIGHT, false, 11);
        document.createParagraph().createRun().setFontSize(2);

        XWPFTable headerTable = document.createTable(8, 1);
        headerTable.setWidth("100%");
        setCellText(headerTable.getRow(0).getCell(0), (metadata.getInstitutionName() != null ? metadata.getInstitutionName() : "KANGEYAM INSTITUTE OF TECHNOLOGY") + " " + (metadata.getTagline() != null ? metadata.getTagline() : "(An Autonomous Institution)"), ParagraphAlignment.CENTER, true, 13);
        String examText = paper.getExamType() != null ? (paper.getExamType().equalsIgnoreCase("INTERNAL_1") ? "CONTINUOUS INTERNAL ASSESSMENT EXAMINATIONS - I" : (paper.getExamType().equalsIgnoreCase("INTERNAL_2") ? "CONTINUOUS INTERNAL ASSESSMENT EXAMINATIONS - II" : "SEMESTER EXAMINATION")) : "EXAMINATION";
        setCellText(headerTable.getRow(1).getCell(0), "B.E. / B.Tech. " + examText, ParagraphAlignment.CENTER, true, 12);
        setCellText(headerTable.getRow(2).getCell(0), metadata.getSemester() != null ? metadata.getSemester() : "", ParagraphAlignment.CENTER, true, 12);
        setCellText(headerTable.getRow(3).getCell(0), metadata.getDepartment() != null ? metadata.getDepartment() : "", ParagraphAlignment.CENTER, true, 12);
        setCellText(headerTable.getRow(4).getCell(0), metadata.getSubjectCodeTitle() != null ? metadata.getSubjectCodeTitle() : subject.getName().toUpperCase(), ParagraphAlignment.CENTER, true, 12);
        setCellText(headerTable.getRow(5).getCell(0), metadata.getCommonTo() != null ? metadata.getCommonTo() : "", ParagraphAlignment.CENTER, false, 11);
        setCellText(headerTable.getRow(6).getCell(0), metadata.getNotes() != null ? metadata.getNotes() : "", ParagraphAlignment.CENTER, false, 11);
        setCellText(headerTable.getRow(7).getCell(0), metadata.getRegulation() != null ? metadata.getRegulation() : "(Regulations 2024)", ParagraphAlignment.CENTER, true, 11);
        for (XWPFTableRow row : headerTable.getRows()) {
            row.setHeight(200);
            row.getCell(0).setVerticalAlignment(XWPFTableCell.XWPFVertAlign.CENTER);
            if (row.getCell(0).getText().isEmpty()) {
                row.getCell(0).getParagraphs().get(0).setSpacingAfter(0);
                row.getCell(0).getParagraphs().get(0).setSpacingBefore(0);
                row.setHeight(0);
            }
        }
        document.createParagraph().createRun().setFontSize(2);

        List<HeaderMetadata.CourseOutcome> cos = metadata.getCourseOutcomes() != null ? metadata.getCourseOutcomes() : List.of();
        if (paper.getExamType() != null) {
            if (paper.getExamType().equalsIgnoreCase("INTERNAL_1")) {
                cos = cos.stream().filter(c -> coInRange(c.getCode(), 1, 3)).collect(Collectors.toList());
            } else if (paper.getExamType().equalsIgnoreCase("INTERNAL_2")) {
                cos = cos.stream().filter(c -> coInRange(c.getCode(), 3, 5)).collect(Collectors.toList());
            }
        }

        if (!cos.isEmpty()) {
            XWPFTable coTable = document.createTable(cos.size() + 1, 2);
            coTable.setWidth("100%");
            CTTblWidth w0 = cellTcPr(coTable.getRow(0).getCell(0)).addNewTcW();
            w0.setW(BigInteger.valueOf(1000));
            w0.setType(STTblWidth.DXA);
            CTTblWidth w1 = cellTcPr(coTable.getRow(0).getCell(1)).addNewTcW();
            w1.setW(BigInteger.valueOf(9000));
            w1.setType(STTblWidth.DXA);
            XWPFTableRow hr = coTable.getRow(0);
            cellTcPr(hr.getCell(0)).addNewHMerge().setVal(STMerge.RESTART);
            cellTcPr(hr.getCell(1)).addNewHMerge().setVal(STMerge.CONTINUE);
            setCellText(hr.getCell(0), "COURSE OUTCOMES (COs): Students will be able to", ParagraphAlignment.LEFT, true, 11);
            int rIdx = 1;
            for (HeaderMetadata.CourseOutcome co : cos) {
                XWPFTableRow r = coTable.getRow(rIdx++);
                setCellText(r.getCell(0), co.getCode() + ":", ParagraphAlignment.LEFT, true, 11);
                setCellText(r.getCell(1), co.getDescription(), ParagraphAlignment.LEFT, false, 11);
            }
        }
        XWPFParagraph sp = document.createParagraph();
        sp.setSpacingBefore(0);
        sp.setSpacingAfter(0);
    }

    static boolean coInRange(String code, int from, int to) {
        if (code == null) {
            return false;
        }
        try {
            int num = Integer.parseInt(code.replaceAll("\\D", ""));
            return num >= from && num <= to;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private CTTcPr cellTcPr(XWPFTableCell cell) {
        return cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
    }

    private void setCellText(XWPFTableCell cell, String text, ParagraphAlignment align, boolean bold, int size) {
        if (cell.getParagraphs().isEmpty()) cell.addParagraph();
        XWPFParagraph p = cell.getParagraphs().get(0);
        p.setAlignment(align);
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        XWPFRun r = p.createRun();
        r.setFontFamily("Times New Roman");
        r.setFontSize(size);
        r.setBold(bold);
        r.setText(text != null ? text : "");
    }

    private void removeTableBorders(XWPFTable table) {
        table.setLeftBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "");
        table.setRightBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "");
        table.setTopBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "");
        table.setBottomBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "");
        table.setInsideHBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "");
        table.setInsideVBorder(XWPFTable.XWPFBorderType.NONE, 0, 0, "");
    }
}