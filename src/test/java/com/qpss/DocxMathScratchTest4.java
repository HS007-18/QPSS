package com.qpss;

import org.apache.poi.xwpf.usermodel.*;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

public class DocxMathScratchTest4 {

    public static String convertOmmlToHtml(String xmlSnippet) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            org.w3c.dom.Document doc = db.parse(new ByteArrayInputStream(xmlSnippet.getBytes(StandardCharsets.UTF_8)));
            Element root = doc.getDocumentElement();
            StringBuilder sb = new StringBuilder();
            walkMath(root, sb);
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    private static void walkMath(Node node, StringBuilder sb) {
        if (node == null) return;

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            String localName = node.getLocalName();
            if ("f".equals(localName)) {
                // Fraction
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
                // Superscript
                Node base = findChild(node, "e");
                Node sup = findChild(node, "sup");
                walkMath(base, sb);
                sb.append("<sup>");
                walkMath(sup, sb);
                sb.append("</sup>");
                return;
            } else if ("sSub".equals(localName)) {
                // Subscript
                Node base = findChild(node, "e");
                Node sub = findChild(node, "sub");
                walkMath(base, sb);
                sb.append("<sub>");
                walkMath(sub, sb);
                sb.append("</sub>");
                return;
            } else if ("sSubSup".equals(localName)) {
                // Subscript and Superscript
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
                String text = node.getTextContent();
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
                        String cellText = cell.getText();
                        if (cellText.contains("Apply the concepts of mobile")) {
                            for (XWPFParagraph para : cell.getParagraphs()) {
                                String xml = para.getCTP().xmlText();
                                if (xml.contains("m:oMath")) {
                                    System.out.println("CONVERTED HTML MATH:\n" + convertOmmlToHtml(xml));
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
