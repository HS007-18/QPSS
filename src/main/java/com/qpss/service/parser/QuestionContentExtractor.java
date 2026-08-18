package com.qpss.service.parser;

import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.openxmlformats.schemas.officeDocument.x2006.math.CTOMath;
import org.openxmlformats.schemas.officeDocument.x2006.math.CTOMathPara;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Base64;
import java.util.List;

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

            // Native XMLBeans Math Extraction (Bulletproof)
            CTP ctp = para.getCTP();
            if (ctp != null) {
                List<CTOMathPara> mathParas = ctp.getOMathParaList();
                if (mathParas != null) {
                    for (CTOMathPara mp : mathParas) {
                        if (mp.getOMathList() != null) {
                            for (CTOMath m : mp.getOMathList()) {
                                appendMathHtml(html, m.getDomNode());
                            }
                        }
                    }
                }
                List<CTOMath> maths = ctp.getOMathList();
                if (maths != null) {
                    for (CTOMath m : maths) {
                        appendMathHtml(html, m.getDomNode());
                    }
                }
            }

            html.append("</p>");
        }
        return html.toString().trim();
    }

    private void appendMathHtml(StringBuilder html, Node domNode) {
        String mathHtml = convertOmmlNodeToHtml(domNode);
        if (mathHtml != null && !mathHtml.isEmpty()) {
            if (html.length() > 0 && !html.toString().endsWith("<br/>") && !html.toString().endsWith("<p>")) {
                html.append("<br/>");
            }
            html.append(mathHtml);
        }
    }

    public static String convertOmmlNodeToHtml(Node node) {
        StringBuilder sb = new StringBuilder();
        walkMath(node, sb);
        return sb.toString();
    }

    private static void walkMath(Node node, StringBuilder sb) {
        if (node == null) return;

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            String localName = node.getLocalName();
            if ("f".equals(localName)) {
                Node numNode = findChild(node, "num");
                Node denNode = findChild(node, "den");
                sb.append("<span style=\"display:inline-block; vertical-align:middle; text-align:center; margin:0 4px;\">");
                sb.append("<span style=\"display:block; border-bottom:1px solid currentColor; padding:0 2px; line-height:1.1;\">");
                walkMath(numNode, sb);
                sb.append("</span>");
                sb.append("<span style=\"display:block; padding:0 2px; line-height:1.1;\">");
                walkMath(denNode, sb);
                sb.append("</span></span>");
                return;
            } else if ("sSup".equals(localName)) {
                Node base = findChild(node, "e");
                Node sup = findChild(node, "sup");
                walkMath(base, sb);
                sb.append("<sup>");
                walkMath(sup, sb);
                sb.append("</sup>");
                return;
            } else if ("sSub".equals(localName)) {
                Node base = findChild(node, "e");
                Node sub = findChild(node, "sub");
                walkMath(base, sb);
                sb.append("<sub>");
                walkMath(sub, sb);
                sb.append("</sub>");
                return;
            } else if ("sSubSup".equals(localName)) {
                Node base = findChild(node, "e");
                Node sub = findChild(node, "sub");
                Node sup = findChild(node, "sup");
                walkMath(base, sb);
                sb.append("<sub>");
                walkMath(sub, sb);
                sb.append("</sub><sup>");
                walkMath(sup, sb);
                sb.append("</sup>");
                return;
            } else if ("t".equals(localName)) {
                String text = getXmlTextContent(node);
                if (text != null) {
                    if (text.equals("=") || text.equals("+") || text.equals("-")) {
                        sb.append(" ").append(escapeHtml(text)).append(" ");
                    } else {
                        sb.append(escapeHtml(text));
                    }
                }
                return;
            }
        }

        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            walkMath(children.item(i), sb);
        }
    }

    private static String getXmlTextContent(Node node) {
        if (node == null) return "";
        if (node.getNodeType() == Node.TEXT_NODE) return node.getNodeValue();
        StringBuilder sb = new StringBuilder();
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            sb.append(getXmlTextContent(children.item(i)));
        }
        return sb.toString();
    }

    private static Node findChild(Node parent, String localName) {
        if (parent == null) return null;
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && localName.equals(child.getLocalName())) {
                return child;
            }
        }
        return null;
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}