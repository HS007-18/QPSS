package com.qpss.documentoutput.export;
import com.qpss.backend.paper.GeneratedPaper;
import com.qpss.backend.paper.PaperQuestion;
import com.qpss.backend.paper.PaperQuestionRepository;
import com.qpss.backend.subject.Subject;
import com.qpss.backend.subject.SubjectRepository;
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
    private final com.qpss.documentoutput.renderer.DocxMasterTableRenderer masterTableRenderer;

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
            footerPara.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.LEFT);
            org.apache.poi.xwpf.usermodel.XWPFRun footerRun1 = footerPara.createRun();
            footerRun1.setText("Page ");
            footerPara.getCTP().addNewFldSimple().setInstr("PAGE");
            org.apache.poi.xwpf.usermodel.XWPFRun footerRun2 = footerPara.createRun();
            footerRun2.setText(" of ");
            footerPara.getCTP().addNewFldSimple().setInstr("NUMPAGES");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate DOCX", e);
        }
    }
}