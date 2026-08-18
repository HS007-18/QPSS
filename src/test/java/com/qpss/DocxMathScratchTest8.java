package com.qpss;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.officeDocument.x2006.math.CTOMath;
import org.openxmlformats.schemas.officeDocument.x2006.math.CTOMathPara;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.FileInputStream;

public class DocxMathScratchTest8 {

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
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    public static void main(String[] args) throws Exception {
        String path = "C:\\Users\\haris\\Downloads\\Part B QB Format.docx";
        try (FileInputStream fis = new FileInputStream(path);
             XWPFDocument doc = new XWPFDocument(fis)) {
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String text = cell.getText();
                        if (text.contains("Apply the concepts of mobile")) {
                            for (XWPFParagraph para : cell.getParagraphs()) {
                                CTP ctp = para.getCTP();
                                for (CTOMathPara mp : ctp.getOMathParaList()) {
                                    for (CTOMath m : mp.getOMathList()) {
                                        System.out.println("RESULT HTML FROM XMLBEANS: " + convertOmmlNodeToHtml(m.getDomNode()));
                                    }
                                }
                                for (CTOMath m : ctp.getOMathList()) {
                                    System.out.println("RESULT HTML FROM XMLBEANS: " + convertOmmlNodeToHtml(m.getDomNode()));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
