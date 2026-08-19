package com.qpss.documentoutput.renderer;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
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
    private HtmlToWordRenderer() {}
    public static void setCellText(XWPFTableCell cell, String text, boolean bold) {
        while (!cell.getParagraphs().isEmpty()) cell.removeParagraph(0);
        XWPFParagraph p = cell.addParagraph();
        p.setSpacingBefore(0);
        p.setSpacingAfter(0);
        XWPFRun r = p.createRun();
        r.setFontFamily("Times New Roman");
        r.setFontSize(11);
        r.setText(text != null ? text : "");
        r.setBold(bold);
    }
    public static void setCellHtml(XWPFTableCell cell, String html) {
        while (!cell.getParagraphs().isEmpty()) cell.removeParagraph(0);
        if (html == null || html.isEmpty()) {
            XWPFParagraph p = cell.addParagraph();
            p.setSpacingBefore(0);
            p.setSpacingAfter(0);
            XWPFRun r = p.createRun();
            r.setFontFamily("Times New Roman");
            r.setFontSize(11);
            r.setText("");
            return;
        }
        org.jsoup.nodes.Document doc = Jsoup.parseBodyFragment(html);
        boolean hasBlock = false;
        for (Node node : doc.body().childNodes()) {
            if (node instanceof Element) {
                Element el = (Element) node;
                String tag = el.tagName().toLowerCase();
                if (tag.equals("p") || tag.equals("div") || tag.equals("blockquote") || tag.startsWith("h")) {
                    XWPFParagraph p = cell.addParagraph();
                    p.setSpacingBefore(0);
                    p.setSpacingAfter(0);
                    p.setAlignment(ParagraphAlignment.BOTH);
                    processHtmlNodes(node, p, false, false, false, false, false);
                    hasBlock = true;
                } else if (tag.equals("ul") || tag.equals("ol")) {
                    int itemIdx = 1;
                    for (Element li : el.select("> li")) {
                        XWPFParagraph p = cell.addParagraph();
                        p.setSpacingBefore(0);
                        p.setSpacingAfter(0);
                        p.setIndentationLeft(200);
                        XWPFRun bulletRun = p.createRun();
                        bulletRun.setFontFamily("Times New Roman");
                        bulletRun.setFontSize(11);
                        bulletRun.setText(tag.equals("ol") ? (itemIdx++) + ". " : "• ");
                        processHtmlNodes(li, p, false, false, false, false, false);
                    }
                    hasBlock = true;
                }
            }
        }
        if (!hasBlock) {
            XWPFParagraph p = cell.addParagraph();
            p.setSpacingBefore(0);
            p.setSpacingAfter(0);
            p.setAlignment(ParagraphAlignment.BOTH);
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
    private static void processHtmlNodes(Node parentNode, XWPFParagraph p, boolean bold, boolean italic, boolean underline, boolean sub, boolean sup) {
        for (Node node : parentNode.childNodes()) {
            if (node instanceof TextNode) {
                TextNode textNode = (TextNode) node;
                String text = textNode.text();
                if (!text.isEmpty()) {
                    XWPFRun run = p.createRun();
                    run.setFontFamily("Times New Roman");
                    run.setFontSize(11);
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
                } else if (tag.equals("span") && (el.hasClass("math-frac") || el.hasClass("fraction"))) {
                    Element num = el.selectFirst(".math-num");
                    Element den = el.selectFirst(".math-den");
                    if (num != null && den != null) {
                        processHtmlNodes(num, p, bold, italic, underline, sub, sup);
                        XWPFRun slashRun = p.createRun();
                        slashRun.setFontFamily("Times New Roman");
                        slashRun.setFontSize(11);
                        slashRun.setText("/");
                        if (bold) slashRun.setBold(true);
                        if (italic) slashRun.setItalic(true);
                        processHtmlNodes(den, p, bold, italic, underline, sub, sup);
                    } else {
                        processHtmlNodes(el, p, bold, italic, underline, sub, sup);
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
    private static void addImage(XWPFParagraph p, String src) {
        if (src == null || !src.startsWith("data:")) return;
        try {
            int commaIdx = src.indexOf(",");
            if (commaIdx <= 0) return;
            String base64 = src.substring(commaIdx + 1);
            String mimeAndEncoding = src.substring(5, commaIdx);
            String mimeType = mimeAndEncoding.split(";")[0];
            byte[] imgData = Base64.getDecoder().decode(base64);
            int pictureType = org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_PNG;
            if (mimeType.contains("jpeg") || mimeType.contains("jpg")) pictureType = org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_JPEG;
            else if (mimeType.contains("gif")) pictureType = org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_GIF;
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
            run.addPicture(new ByteArrayInputStream(imgData), pictureType, "image", Units.toEMU(width), Units.toEMU(height));
        } catch (Exception e) {
            log.warn("Image embedding error", e);
        }
    }
}