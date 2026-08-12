package com.qpss.service;

import com.qpss.model.GeneratedPaper;
import com.qpss.model.PaperQuestion;
import com.qpss.model.Question;
import com.qpss.model.Subject;
import com.qpss.repository.PaperQuestionRepository;
import com.qpss.repository.QuestionRepository;
import com.qpss.repository.SubjectRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.springframework.stereotype.Service;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocxRendererService {

    private final PaperQuestionRepository paperQuestionRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;

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
            
            // Header
            XWPFParagraph headerPara = document.createParagraph();
            headerPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun headerRun = headerPara.createRun();
            headerRun.setBold(true);
            headerRun.setFontSize(16);
            headerRun.setText("COLLEGE OF ENGINEERING");
            headerRun.addCarriageReturn();
            
            XWPFRun examRun = headerPara.createRun();
            examRun.setBold(true);
            examRun.setFontSize(14);
            examRun.setText(paper.getExamType().toUpperCase().replace("_", " ") + " EXAMINATIONS");
            examRun.addCarriageReturn();
            
            XWPFRun subjectRun = headerPara.createRun();
            subjectRun.setBold(true);
            subjectRun.setFontSize(14);
            subjectRun.setText(subject.getName());
            
            document.createParagraph().createRun().addBreak();

            // PART A
            XWPFParagraph partAPara = document.createParagraph();
            partAPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun partARun = partAPara.createRun();
            partARun.setBold(true);
            partARun.setText("PART A - (10 x 2 = 20 marks)");
            
            XWPFTable tableA = document.createTable(sectionA.size() + 1, 4);
            tableA.setWidth("100%");
            
            // Header row Part A
            setCellText(tableA.getRow(0).getCell(0), "Q.No.", true);
            setCellText(tableA.getRow(0).getCell(1), "Question", true);
            setCellText(tableA.getRow(0).getCell(2), "M", true);
            setCellText(tableA.getRow(0).getCell(3), "CO", true);

            for (int i = 0; i < sectionA.size(); i++) {
                PaperQuestion pq = sectionA.get(i);
                Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
                if (q == null) continue;
                
                XWPFTableRow row = tableA.getRow(i + 1);
                setCellText(row.getCell(0), String.valueOf(pq.getQuestionNumber()), false);
                setCellHtml(row.getCell(1), q.getQuestionContent());
                setCellText(row.getCell(2), "2", false);
                setCellText(row.getCell(3), q.getCo() != null && q.getCo().toUpperCase().startsWith("CO") ? q.getCo() : "CO" + q.getCo(), false);
            }
            
            document.createParagraph().createRun().addBreak();
            
            // PART B
            XWPFParagraph partBPara = document.createParagraph();
            partBPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun partBRun = partBPara.createRun();
            partBRun.setBold(true);
            partBRun.setText("PART B - (5 x 16 = 80 marks)");

            XWPFTable tableB = document.createTable(sectionB.size() + 1 + (sectionB.size()/2), 4);
            tableB.setWidth("100%");
            
            // Header row Part B
            setCellText(tableB.getRow(0).getCell(0), "Q.No.", true);
            setCellText(tableB.getRow(0).getCell(1), "Question", true);
            setCellText(tableB.getRow(0).getCell(2), "M", true);
            setCellText(tableB.getRow(0).getCell(3), "CO", true);
            
            int rowIndex = 1;
            Integer currentQNum = null;
            for (int i = 0; i < sectionB.size(); i++) {
                PaperQuestion pq = sectionB.get(i);
                Question q = questionRepository.findById(pq.getQuestionId()).orElse(null);
                if (q == null) continue;

                if (currentQNum != null && currentQNum.equals(pq.getQuestionNumber())) {
                    XWPFTableRow orRow = tableB.getRow(rowIndex++);
                    mergeCellsHorizontal(orRow, 0, 3);
                    XWPFParagraph orPara = orRow.getCell(0).getParagraphs().get(0);
                    orPara.setAlignment(ParagraphAlignment.CENTER);
                    XWPFRun orRun = orPara.createRun();
                    orRun.setBold(true);
                    orRun.setText("(OR)");
                } else {
                    currentQNum = pq.getQuestionNumber();
                }

                XWPFTableRow row = tableB.getRow(rowIndex++);
                String label = pq.getChoiceLabel() != null ? pq.getChoiceLabel() + ")" : "";
                setCellText(row.getCell(0), pq.getQuestionNumber() + " " + label, false);
                setCellHtml(row.getCell(1), q.getQuestionContent());
                setCellText(row.getCell(2), String.valueOf(q.getMarks()), false);
                setCellText(row.getCell(3), q.getCo() != null && q.getCo().toUpperCase().startsWith("CO") ? q.getCo() : "CO" + q.getCo(), false);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate DOCX", e);
        }
    }
    
    private void setCellText(XWPFTableCell cell, String text, boolean bold) {
        if (cell.getParagraphs().isEmpty()) {
            cell.addParagraph();
        }
        XWPFParagraph p = cell.getParagraphs().get(0);
        XWPFRun r = p.createRun();
        r.setText(text != null ? text : "");
        r.setBold(bold);
    }

    private void setCellHtml(XWPFTableCell cell, String html) {
        while (!cell.getParagraphs().isEmpty()) {
            cell.removeParagraph(0);
        }

        if (html == null || html.isEmpty()) {
            cell.addParagraph().createRun().setText("");
            return;
        }

        org.jsoup.nodes.Document doc = Jsoup.parseBodyFragment(html);
        for (Element pElement : doc.body().children()) {
            XWPFParagraph p = cell.addParagraph();
            if ("p".equalsIgnoreCase(pElement.tagName())) {
                processHtmlNodes(pElement, p, false, false, false, false, false);
            } else {
                processHtmlNodes(doc.body(), p, false, false, false, false, false);
                break;
            }
        }
    }

    private void processHtmlNodes(Node parentNode, XWPFParagraph p, 
                                  boolean bold, boolean italic, boolean underline, 
                                  boolean sub, boolean sup) {
        for (Node node : parentNode.childNodes()) {
            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                String text = textNode.text();
                if (!text.isEmpty()) {
                    XWPFRun run = p.createRun();
                    run.setText(text);
                    if (bold) run.setBold(true);
                    if (italic) run.setItalic(true);
                    if (underline) run.setUnderline(UnderlinePatterns.SINGLE);
                    if (sub) run.setSubscript(VerticalAlign.SUBSCRIPT);
                    if (sup) run.setSubscript(VerticalAlign.SUPERSCRIPT);
                }
            } else if (node instanceof Element) {
                Element el = (Element) node;
                String tag = el.tagName().toLowerCase();
                
                if (tag.equals("br")) {
                    p.createRun().addBreak();
                } else if (tag.equals("img")) {
                    String src = el.attr("src");
                    if (src.startsWith("data:")) {
                        try {
                            int commaIdx = src.indexOf(",");
                            if (commaIdx > 0) {
                                String base64 = src.substring(commaIdx + 1);
                                String mimeAndEncoding = src.substring(5, commaIdx);
                                String mimeType = mimeAndEncoding.split(";")[0];
                                
                                byte[] imgData = Base64.getDecoder().decode(base64);
                                int pictureType = org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_PNG;
                                if (mimeType.contains("jpeg") || mimeType.contains("jpg")) {
                                    pictureType = org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_JPEG;
                                } else if (mimeType.contains("gif")) {
                                    pictureType = org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_GIF;
                                }
                                
                                XWPFRun run = p.createRun();
                                ByteArrayInputStream bis = new ByteArrayInputStream(imgData);
                                BufferedImage bimg = ImageIO.read(bis);
                                int width = 200;
                                int height = 200;
                                if (bimg != null) {
                                    width = bimg.getWidth();
                                    height = bimg.getHeight();
                                    if (width > 300) {
                                        double ratio = 300.0 / width;
                                        width = 300;
                                        height = (int) (height * ratio);
                                    }
                                }
                                ByteArrayInputStream bisForPoi = new ByteArrayInputStream(imgData);
                                run.addPicture(bisForPoi, pictureType, "image", org.apache.poi.util.Units.toEMU(width), org.apache.poi.util.Units.toEMU(height));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    processHtmlNodes(el, p, 
                        bold || tag.equals("b") || tag.equals("strong"),
                        italic || tag.equals("i") || tag.equals("em"),
                        underline || tag.equals("u"),
                        sub || tag.equals("sub"),
                        sup || tag.equals("sup")
                    );
                }
            }
        }
    }
    
    private void mergeCellsHorizontal(XWPFTableRow row, int fromCol, int toCol) {
        for (int i = fromCol; i <= toCol; i++) {
            XWPFTableCell cell = row.getCell(i);
            CTTcPr tcPr = cell.getCTTc().isSetTcPr() ? cell.getCTTc().getTcPr() : cell.getCTTc().addNewTcPr();
            if (i == fromCol) {
                tcPr.addNewHMerge().setVal(STMerge.RESTART);
            } else {
                tcPr.addNewHMerge().setVal(STMerge.CONTINUE);
            }
        }
    }
}
