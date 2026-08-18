package com.qpss.generation.renderer;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.VerticalAlign;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STMerge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Base64;

public final class HtmlToWordRenderer {

    private static final Logger log = LoggerFactory.getLogger(HtmlToWordRenderer.class);

    private HtmlToWordRenderer() {
    }

    public static void setCellText(XWPFTableCell cell, String text, boolean bold) {
        if (cell.getParagraphs().isEmpty()) {
            cell.addParagraph();
        }
        XWPFParagraph p = cell.getParagraphs().get(0);
        XWPFRun r = p.createRun();
        r.setText(text != null ? text : "");
        r.setBold(bold);
    }

    public static void setCellHtml(XWPFTableCell cell, String html) {
        while (!cell.getParagraphs().isEmpty()) {
            cell.removeParagraph(0);
        }

        if (html == null || html.isEmpty()) {
            cell.addParagraph().createRun().setText("");
            return;
        }

        org.jsoup.nodes.Document doc = Jsoup.parseBodyFragment(html);
        boolean created = false;
        for (Node node : doc.body().childNodes()) {
            if (node instanceof Element && "p".equalsIgnoreCase(((Element) node).tagName())) {
                XWPFParagraph p = cell.addParagraph();
                p.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.BOTH);
                processHtmlNodes(node, p, false, false, false, false, false);
                created = true;
            }
        }
        if (!created) {
            XWPFParagraph p = cell.addParagraph();
            p.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.BOTH);
            processHtmlNodes(doc.body(), p, false, false, false, false, false);
        }
    }

    public static void mergeCellsHorizontal(XWPFTableRow row, int fromCol, int toCol) {
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

    private static void processHtmlNodes(Node parentNode, XWPFParagraph p,
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
                    addImage(p, el.attr("src"));
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

    private static void addImage(XWPFParagraph p, String src) {
        if (src == null || !src.startsWith("data:")) {
            return;
        }
        try {
            int commaIdx = src.indexOf(",");
            if (commaIdx <= 0) {
                return;
            }
            String base64 = src.substring(commaIdx + 1);
            String mimeAndEncoding = src.substring(5, commaIdx);
            String mimeType = mimeAndEncoding.split(";")[0];

            byte[] imgData = Base64.getDecoder().decode(base64);
            int pictureType = Document.PICTURE_TYPE_PNG;
            if (mimeType.contains("jpeg") || mimeType.contains("jpg")) {
                pictureType = Document.PICTURE_TYPE_JPEG;
            } else if (mimeType.contains("gif")) {
                pictureType = Document.PICTURE_TYPE_GIF;
            }

            XWPFRun run = p.createRun();
            BufferedImage bimg = ImageIO.read(new ByteArrayInputStream(imgData));
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
            run.addPicture(new ByteArrayInputStream(imgData), pictureType, "image",
                    Units.toEMU(width), Units.toEMU(height));
        } catch (Exception e) {
            log.warn("Failed to embed image in Word document", e);
        }
    }
}