package com.qpss.service.docx;

import com.qpss.model.GeneratedPaper;
import com.qpss.model.HeaderMetadata;
import com.qpss.model.PaperQuestion;
import com.qpss.model.Question;
import com.qpss.model.Subject;
import com.qpss.repository.QuestionRepository;
import com.qpss.repository.SourceDocumentRepository;

import com.qpss.service.SourceDocumentStorageService;
import com.qpss.service.parser.HeaderMetadataExtractor;
import lombok.RequiredArgsConstructor;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DocxHeaderRenderer {

    private static final String DEFAULT_INSTITUTE = "KANGEYAM INSTITUTE OF TECHNOLOGY";
    private static final String DEFAULT_TAGLINE = "(An Autonomous Institution)";

    private final QuestionRepository questionRepository;
    private final SourceDocumentRepository sourceDocumentRepository;
    private final SourceDocumentStorageService storageService;
    private final HeaderMetadataExtractor metadataExtractor = new HeaderMetadataExtractor();

    public void render(XWPFDocument document, GeneratedPaper paper, Subject subject,
                       List<PaperQuestion> sectionA, List<PaperQuestion> sectionB) {

        Long sourceDocId = null;
        if (sectionA != null && !sectionA.isEmpty()) {
            Question q = questionRepository.findById(sectionA.get(0).getQuestionId()).orElse(null);
            if (q != null) sourceDocId = q.getSourceDocumentId();
        } else if (sectionB != null && !sectionB.isEmpty()) {
            Question q = questionRepository.findById(sectionB.get(0).getQuestionId()).orElse(null);
            if (q != null) sourceDocId = q.getSourceDocumentId();
        }

        HeaderMetadata metadata = null;

        if (sourceDocId != null) {
            com.qpss.model.SourceDocument sourceDoc = sourceDocumentRepository.findById(sourceDocId).orElse(null);
            if (sourceDoc != null) {
                try {
                    byte[] fileBytes = storageService.loadDocument(sourceDoc.getStoredFileName());
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes);
                         XWPFDocument sourceDocx = new XWPFDocument(bis)) {
                        metadata = metadataExtractor.extract(sourceDocx);
                    }
                } catch (Exception e) {
                    // Fallback to default metadata
                }
            }
        }

        if (metadata == null) {
            metadata = new HeaderMetadata();
        }

        renderStandardizedHeader(document, paper, subject, metadata);
    }

    private void renderStandardizedHeader(XWPFDocument document, GeneratedPaper paper, Subject subject, HeaderMetadata metadata) {
        // 1. Top Candidate Registration Line
        XWPFParagraph topPara = document.createParagraph();
        topPara.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun topRun = topPara.createRun();
        topRun.setFontFamily("Times New Roman");
        topRun.setBold(true);
        topRun.setFontSize(10);
        topRun.setText("QP CODE:                                        Date:                                        Register No.: ..........................");

        // 2. Header Content Block (Centered)
        XWPFParagraph headerPara = document.createParagraph();
        headerPara.setAlignment(ParagraphAlignment.CENTER);


        // Institution Name
        XWPFRun instRun = headerPara.createRun();
        instRun.setFontFamily("Times New Roman");
        instRun.setBold(true);
        instRun.setFontSize(16);
        instRun.setText(metadata.getInstitutionName() != null ? metadata.getInstitutionName() : DEFAULT_INSTITUTE);
        instRun.addCarriageReturn();

        // Tagline
        XWPFRun autoRun = headerPara.createRun();
        autoRun.setFontFamily("Times New Roman");
        autoRun.setBold(true);
        autoRun.setFontSize(11);
        autoRun.setText(metadata.getTagline() != null ? metadata.getTagline() : DEFAULT_TAGLINE);
        autoRun.addCarriageReturn();

        // Exam Title
        XWPFRun assessRun = headerPara.createRun();
        assessRun.setFontFamily("Times New Roman");
        assessRun.setBold(true);
        assessRun.setFontSize(13);
        String examText = paper.getExamType() != null ? paper.getExamType().toUpperCase().replace("_", " ") : "CONTINUOUS INTERNAL ASSESSMENT EXAMINATIONS - I";
        assessRun.setText(examText);
        assessRun.addCarriageReturn();

        // Semester
        if (metadata.getSemester() != null) {
            XWPFRun semRun = headerPara.createRun();
            semRun.setFontFamily("Times New Roman");
            semRun.setBold(true);
            semRun.setFontSize(11);
            semRun.setText(metadata.getSemester());
            semRun.addCarriageReturn();
        }

        // Department
        if (metadata.getDepartment() != null) {
            XWPFRun deptRun = headerPara.createRun();
            deptRun.setFontFamily("Times New Roman");
            deptRun.setBold(true);
            deptRun.setFontSize(11);
            deptRun.setText(metadata.getDepartment());
            deptRun.addCarriageReturn();
        }

        // Subject Code & Title
        XWPFRun subRun = headerPara.createRun();
        subRun.setFontFamily("Times New Roman");
        subRun.setBold(true);
        subRun.setFontSize(12);
        String subTitle = metadata.getSubjectCodeTitle() != null ? metadata.getSubjectCodeTitle() : subject.getName().toUpperCase();
        subRun.setText(subTitle);
        subRun.addCarriageReturn();

        // Common To
        if (metadata.getCommonTo() != null) {
            XWPFRun commRun = headerPara.createRun();
            commRun.setFontFamily("Times New Roman");
            commRun.setFontSize(10);
            commRun.setText(metadata.getCommonTo());
            commRun.addCarriageReturn();
        }

        // Notes
        if (metadata.getNotes() != null) {
            XWPFRun noteRun = headerPara.createRun();
            noteRun.setFontFamily("Times New Roman");
            noteRun.setFontSize(10);
            noteRun.setText(metadata.getNotes());
            noteRun.addCarriageReturn();
        }

        // Regulation
        if (metadata.getRegulation() != null) {
            XWPFRun regRun = headerPara.createRun();
            regRun.setFontFamily("Times New Roman");
            regRun.setBold(true);
            regRun.setFontSize(10);
            regRun.setText(metadata.getRegulation());
            regRun.addCarriageReturn();
        }

        // 3. Course Outcomes Section
        if (metadata.getCourseOutcomes() != null && !metadata.getCourseOutcomes().isEmpty()) {
            XWPFParagraph coHeaderPara = document.createParagraph();
            XWPFRun coHeadRun = coHeaderPara.createRun();
            coHeadRun.setFontFamily("Times New Roman");
            coHeadRun.setBold(true);
            coHeadRun.setFontSize(10);
            coHeadRun.setText("COURSE OUTCOMES (COs): Students will be able to");

            for (HeaderMetadata.CourseOutcome co : metadata.getCourseOutcomes()) {
                XWPFParagraph coPara = document.createParagraph();
                coPara.setIndentationLeft(200);
                
                XWPFRun codeRun = coPara.createRun();
                codeRun.setFontFamily("Times New Roman");
                codeRun.setBold(true);
                codeRun.setFontSize(10);
                codeRun.setText(co.getCode() + ": ");

                XWPFRun descRun = coPara.createRun();
                descRun.setFontFamily("Times New Roman");
                descRun.setFontSize(10);
                descRun.setText(co.getDescription());
            }
        }

        // Divider spacing before Part A
        document.createParagraph().createRun().setFontSize(6);
    }
}