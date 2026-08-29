package com.qpss.service;

import com.qpss.document.renderer.DocxMasterTableRenderer;
import com.qpss.entity.GeneratedPaper;
import com.qpss.entity.PaperQuestion;
import com.qpss.repository.PaperQuestionRepository;
import com.qpss.entity.Subject;
import com.qpss.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class DocxRendererService {

    private final PaperQuestionRepository paperQuestionRepository;
    private final SubjectRepository subjectRepository;
    private final com.qpss.document.renderer.DocxMasterTableRenderer masterTableRenderer;

    public byte[] exportPaperToDocx(GeneratedPaper paper) {
        Subject subject = subjectRepository.findById(paper.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        List<PaperQuestion> pqs = paperQuestionRepository.findByPaperIdOrderByQuestionNumberAscChoiceLabelAsc(paper.getId());

        List<PaperQuestion> sectionA = pqs.stream()
                .filter(q -> "SECTION_A".equals(q.getSection()))
                .sorted((q1, q2) -> q1.getQuestionNumber().compareTo(q2.getQuestionNumber()))
                .collect(Collectors.toList());

        List<PaperQuestion> sectionB = pqs.stream()
                .filter(q -> "SECTION_B".equals(q.getSection()))
                .sorted((q1, q2) -> {
                    int numCmp = q1.getQuestionNumber().compareTo(q2.getQuestionNumber());
                    if (numCmp != 0) return numCmp;
                    if (q1.getChoiceLabel() == null) return -1;
                    if (q2.getChoiceLabel() == null) return 1;
                    return q1.getChoiceLabel().compareTo(q2.getChoiceLabel());
                })
                .collect(Collectors.toList());

        try (XWPFDocument document = new XWPFDocument()) {
            masterTableRenderer.renderMasterTable(document, paper, subject, sectionA, sectionB);

            org.apache.poi.xwpf.usermodel.XWPFFooter footer = document.createFooter(org.apache.poi.wp.usermodel.HeaderFooterType.DEFAULT);
            org.apache.poi.xwpf.usermodel.XWPFParagraph footerPara = footer.createParagraph();
            footerPara.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.RIGHT);
            
            org.apache.poi.xwpf.usermodel.XWPFRun footerRun1 = footerPara.createRun();
            footerRun1.setFontFamily("Times New Roman");
            footerRun1.setFontSize(10);
            footerRun1.setText("Page ");
            
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField fld1 = footerPara.getCTP().addNewFldSimple();
            fld1.setInstr("PAGE \\* MERGEFORMAT");
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR r1 = fld1.addNewR();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr rPr1 = r1.addNewRPr();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts f1 = rPr1.addNewRFonts();
            f1.setAscii("Times New Roman");
            f1.setHAnsi("Times New Roman");
            rPr1.addNewSz().setVal(java.math.BigInteger.valueOf(20));
            
            org.apache.poi.xwpf.usermodel.XWPFRun footerRun2 = footerPara.createRun();
            footerRun2.setFontFamily("Times New Roman");
            footerRun2.setFontSize(10);
            footerRun2.setText(" of ");
            
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField fld2 = footerPara.getCTP().addNewFldSimple();
            fld2.setInstr("NUMPAGES \\* MERGEFORMAT");
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTR r2 = fld2.addNewR();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr rPr2 = r2.addNewRPr();
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts f2 = rPr2.addNewRFonts();
            f2.setAscii("Times New Roman");
            f2.setHAnsi("Times New Roman");
            rPr2.addNewSz().setVal(java.math.BigInteger.valueOf(20));

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate DOCX", e);
        }
    }
}