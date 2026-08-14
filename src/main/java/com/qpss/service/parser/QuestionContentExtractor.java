package com.qpss.service.parser;

import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;

import java.util.Base64;

public class QuestionContentExtractor {

    public String extractRichContent(XWPFTableCell cell, XWPFDocument document) {
        if (cell == null) {
            return "";
        }
        StringBuilder html = new StringBuilder();
        for (XWPFParagraph para : cell.getParagraphs()) {
            html.append("<p>");
            for (XWPFRun run : para.getRuns()) {
                for (XWPFPicture pic : run.getEmbeddedPictures()) {
                    XWPFPictureData picData = pic.getPictureData();
                    String base64 = Base64.getEncoder().encodeToString(picData.getData());
                    String mimeType = picData.getPackagePart().getContentType();
                    html.append("<img src=\"data:").append(mimeType)
                            .append(";base64,").append(base64).append("\" />");
                }
                String text = run.getText(0);
                if (text != null && !text.isEmpty()) {
                    if (run.isBold()) html.append("<b>");
                    if (run.isItalic()) html.append("<i>");
                    if (run.getUnderline() != UnderlinePatterns.NONE) html.append("<u>");
                    String vAlign = run.getVerticalAlignment() == null ? "" : run.getVerticalAlignment().toString();
                    if (vAlign.equals("subscript")) html.append("<sub>");
                    if (vAlign.equals("superscript")) html.append("<sup>");

                    html.append(escapeHtml(text));

                    if (vAlign.equals("superscript")) html.append("</sup>");
                    if (vAlign.equals("subscript")) html.append("</sub>");
                    if (run.getUnderline() != UnderlinePatterns.NONE) html.append("</u>");
                    if (run.isItalic()) html.append("</i>");
                    if (run.isBold()) html.append("</b>");
                }
                int brCount = 0;
                if (run.getCTR() != null && run.getCTR().getBrList() != null) {
                    brCount = run.getCTR().getBrList().size();
                }
                for (int b = 0; b < brCount; b++) {
                    html.append("<br/>");
                }
            }
            html.append("</p>");
        }
        return html.toString().trim();
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}