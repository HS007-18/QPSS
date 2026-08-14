package com.qpss.service.docx;

import com.qpss.model.GeneratedPaper;
import com.qpss.model.PaperQuestion;
import com.qpss.model.Question;
import com.qpss.model.Subject;
import com.qpss.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Component
@RequiredArgsConstructor
public class DocxHeaderRenderer {

    private static final String INSTITUTE_NAME = "KANGEYAM INSTITUTE OF TECHNOLOGY";
    private static final String INSTITUTE_TAGLINE = "(An Autonomous Institution)";

    private final QuestionRepository questionRepository;

    public void render(XWPFDocument document, GeneratedPaper paper, Subject subject,
                       List<PaperQuestion> sectionA, List<PaperQuestion> sectionB) {
        XWPFParagraph topPara = document.createParagraph();
        topPara.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun topRun = topPara.createRun();
        topRun.setBold(true);
        topRun.setFontSize(11);
        topRun.setText("QP CODE:                                        Date:                                        Register No.: ..........................");

        XWPFParagraph headerPara = document.createParagraph();
        headerPara.setAlignment(ParagraphAlignment.CENTER);

        XWPFRun instRun = headerPara.createRun();
        instRun.setBold(true);
        instRun.setFontSize(16);
        instRun.setText(INSTITUTE_NAME);
        instRun.addCarriageReturn();

        XWPFRun autoRun = headerPara.createRun();
        autoRun.setBold(true);
        autoRun.setFontSize(12);
        autoRun.setText(INSTITUTE_TAGLINE);
        autoRun.addCarriageReturn();

        XWPFRun assessRun = headerPara.createRun();
        assessRun.setBold(true);
        assessRun.setFontSize(14);
        assessRun.setText(paper.getExamType().toUpperCase().replace("_", " ") + " - [ I / II ]");
        assessRun.addCarriageReturn();

        XWPFRun subRun = headerPara.createRun();
        subRun.setBold(true);
        subRun.setFontSize(14);
        subRun.setText("[ SUBJECT CODE ] - " + subject.getName().toUpperCase());
        subRun.addCarriageReturn();

        XWPFRun regRun = headerPara.createRun();
        regRun.setBold(true);
        regRun.setFontSize(12);
        regRun.setText("(Regulation ");
        XWPFRun regRunRed = headerPara.createRun();
        regRunRed.setBold(true);
        regRunRed.setFontSize(12);
        regRunRed.setText("[ 202X ]");
        XWPFRun regRunEnd = headerPara.createRun();
        regRunEnd.setBold(true);
        regRunEnd.setFontSize(12);
        regRunEnd.setText(")");
        regRunEnd.addCarriageReturn();

        XWPFRun semRunRed = headerPara.createRun();
        semRunRed.setBold(true);
        semRunRed.setFontSize(12);
        semRunRed.setText("[ I / II / III ]");
        XWPFRun semRun = headerPara.createRun();
        semRun.setBold(true);
        semRun.setFontSize(12);
        semRun.setText(" Semester");
        semRun.addCarriageReturn();

        XWPFRun deptRun = headerPara.createRun();
        deptRun.setBold(true);
        deptRun.setFontSize(12);
        deptRun.setText("[ B.E. Department Name ]");
        deptRun.addCarriageReturn();

        XWPFRun commRun = headerPara.createRun();
        commRun.setFontSize(12);
        commRun.setText("Common to: ");
        XWPFRun commRunRed = headerPara.createRun();
        commRunRed.setFontSize(12);
        commRunRed.setText("[ Branches / NIL ]");
        commRunRed.addCarriageReturn();

        XWPFRun noteRun = headerPara.createRun();
        noteRun.setFontSize(12);
        noteRun.setText("Note: [ Enter Note Here ]");
        noteRun.addCarriageReturn();

        renderCoTable(document, sectionA, sectionB);
    }

    private void renderCoTable(XWPFDocument document,
                               List<PaperQuestion> sectionA, List<PaperQuestion> sectionB) {
        Set<String> uniqueCos = new TreeSet<>();
        collectCos(uniqueCos, sectionA);
        collectCos(uniqueCos, sectionB);

        if (uniqueCos.isEmpty()) {
            return;
        }

        XWPFTable coTable = document.createTable(uniqueCos.size(), 2);
        coTable.setWidth("100%");

        int rIdx = 0;
        for (String co : uniqueCos) {
            XWPFTableRow r = coTable.getRow(rIdx++);
            XWPFTableCell c0 = r.getCell(0);
            c0.setWidth("15%");
            if (c0.getParagraphs().isEmpty()) c0.addParagraph();
            XWPFParagraph cp0 = c0.getParagraphs().get(0);
            cp0.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun cr0 = cp0.createRun();
            cr0.setBold(true);
            cr0.setText(co);

            XWPFTableCell c1 = r.getCell(1);
            c1.setWidth("85%");
        }

        document.createParagraph().createRun().addBreak();
    }

    private void collectCos(Set<String> uniqueCos, List<PaperQuestion> paperQuestions) {
        for (PaperQuestion pq : paperQuestions) {
            Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
            if (q != null && q.getCo() != null) {
                uniqueCos.add(q.getCo().toUpperCase().startsWith("CO") ? q.getCo() : "CO" + q.getCo());
            }
        }
    }
}