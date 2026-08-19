package com.qpss.documentextraction.extractor;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.officeDocument.x2006.math.CTOMath;
import org.openxmlformats.schemas.officeDocument.x2006.math.CTOMathPara;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTText;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.Base64;
import java.util.List;
public class QuestionContentExtractor {
    public String extractRichContent(XWPFTableCell cell, XWPFDocument document) {
        if (cell == null) return "";
        StringBuilder html = new StringBuilder();
        for (XWPFParagraph para : cell.getParagraphs()) {
            html.append("<p>");
            for (XWPFRun run : para.getRuns()) {
                appendRunHtml(html, run);
            }
            CTP ctp = para.getCTP();
            if (ctp != null) {
                List<CTOMathPara> mathParas = ctp.getOMathParaList();
                if (mathParas != null) {
                    for (CTOMathPara mp : mathParas) {
                        if (mp.getOMathList() != null) {
                            for (CTOMath m : mp.getOMathList()) walkMath(m.getDomNode(), html);
                        }
                    }
                }
                List<CTOMath> maths = ctp.getOMathList();
                if (maths != null) {
                    for (CTOMath m : maths) walkMath(m.getDomNode(), html);
                }
            }
            html.append("</p>");
        }
        return html.toString().trim();
    }
    private void appendRunHtml(StringBuilder html, XWPFRun run) {
        if (run == null) return;
        int brCount = (run.getCTR() != null && run.getCTR().getBrList() != null) ? run.getCTR().getBrList().size() : 0;
        for (int b = 0; b < brCount; b++) html.append("<br/>");
        for (XWPFPicture pic : run.getEmbeddedPictures()) {
            XWPFPictureData picData = pic.getPictureData();
            String base64 = Base64.getEncoder().encodeToString(picData.getData());
            String mimeType = picData.getPackagePart().getContentType();
            html.append("<img src=\"data:").append(mimeType).append(";base64,").append(base64).append("\" />");
        }
        String text = run.text();
        if (text == null || text.isEmpty()) {
            if (run.getCTR() != null && run.getCTR().getTList() != null) {
                StringBuilder sb = new StringBuilder();
                for (CTText t : run.getCTR().getTList()) sb.append(t.getStringValue());
                text = sb.toString();
            }
        }
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
    }
    public static String convertOmmlNodeToHtml(Node node) {
        StringBuilder sb = new StringBuilder();
        walkMath(node, sb);
        return sb.toString();
    }
    private static void walkMath(Node node, StringBuilder sb) {
        if (node == null) return;
        if (node.getNodeType() == Node.ELEMENT_NODE) {
            String tagName = getTagName(node);
            if ("f".equals(tagName)) {
                Node numNode = findChild(node, "num");
                Node denNode = findChild(node, "den");
                sb.append("<span class=\"math-frac\" style=\"display:inline-block; vertical-align:middle; text-align:center; padding:0 2px;\">");
                sb.append("<span class=\"math-num\" style=\"display:block; border-bottom:1px solid currentColor; padding:0 2px; line-height:1.1;\">");
                walkMath(numNode, sb);
                sb.append("</span>");
                sb.append("<span class=\"math-den\" style=\"display:block; padding:0 2px; line-height:1.1;\">");
                walkMath(denNode, sb);
                sb.append("</span></span>");
                return;
            } else if ("sSup".equals(tagName)) {
                Node base = findChild(node, "e");
                Node sup = findChild(node, "sup");
                walkMath(base, sb);
                sb.append("<sup>");
                walkMath(sup, sb);
                sb.append("</sup>");
                return;
            } else if ("sSub".equals(tagName)) {
                Node base = findChild(node, "e");
                Node sub = findChild(node, "sub");
                walkMath(base, sb);
                sb.append("<sub>");
                walkMath(sub, sb);
                sb.append("</sub>");
                return;
            } else if ("sSubSup".equals(tagName)) {
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
            } else if ("d".equals(tagName)) {
                Node dPr = findChild(node, "dPr");
                String beg = "(";
                String end = ")";
                if (dPr != null) {
                    Node begNode = findChild(dPr, "begChr");
                    if (begNode != null && begNode.getAttributes() != null && begNode.getAttributes().getNamedItem("m:val") != null) {
                        beg = begNode.getAttributes().getNamedItem("m:val").getNodeValue();
                    }
                    Node endNode = findChild(dPr, "endChr");
                    if (endNode != null && endNode.getAttributes() != null && endNode.getAttributes().getNamedItem("m:val") != null) {
                        end = endNode.getAttributes().getNamedItem("m:val").getNodeValue();
                    }
                }
                sb.append(escapeHtml(beg));
                walkMath(findChild(node, "e"), sb);
                sb.append(escapeHtml(end));
                return;
            } else if ("rad".equals(tagName)) {
                sb.append("√(");
                walkMath(findChild(node, "e"), sb);
                sb.append(")");
                return;
            } else if ("t".equals(tagName)) {
                String text = getXmlTextContent(node);
                if (text != null && !text.isEmpty()) {
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
    private static String getTagName(Node node) {
        if (node == null) return "";
        String name = node.getLocalName();
        if (name == null || name.isEmpty()) name = node.getNodeName();
        if (name != null && name.contains(":")) name = name.substring(name.indexOf(":") + 1);
        return name != null ? name : "";
    }
    private static Node findChild(Node parent, String targetName) {
        if (parent == null) return null;
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && targetName.equalsIgnoreCase(getTagName(child))) {
                return child;
            }
        }
        return null;
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
    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}