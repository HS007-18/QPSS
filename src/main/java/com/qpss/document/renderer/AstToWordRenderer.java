package com.qpss.document.renderer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qpss.document.model.ast.*;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;
import org.apache.xmlbeans.XmlObject;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.officeDocument.x2006.math.CTOMath;

public class AstToWordRenderer {
    private static final Logger log = LoggerFactory.getLogger(AstToWordRenderer.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void setCellAst(XWPFTableCell cell, String structuredContent) {
        while (!cell.getParagraphs().isEmpty()) {
            cell.removeParagraph(0);
        }

        if (structuredContent == null || structuredContent.isEmpty()) {
            cell.addParagraph().createRun().setText("");
            return;
        }

        try {
            List<AstNode> nodes = mapper.readValue(structuredContent, new TypeReference<List<AstNode>>() {});
            for (AstNode node : nodes) {
                renderNode(node, cell);
            }
            if (cell.getParagraphs().isEmpty() || (!nodes.isEmpty() && nodes.get(nodes.size() - 1) instanceof TableNode)) {
                cell.addParagraph();
            }
        } catch (Exception e) {
            log.error("Failed to parse or render structured content", e);
            cell.addParagraph().createRun().setText("[Error rendering question content]");
        }
    }

    private static void renderNode(AstNode node, XWPFTableCell cell) {
        if (node instanceof ParagraphNode) {
            ParagraphNode pNode = (ParagraphNode) node;
            XWPFParagraph p = cell.addParagraph();
            
            try {
                if (pNode.getAlignment() != null) {
                    p.setAlignment(ParagraphAlignment.valueOf(pNode.getAlignment()));
                }
            } catch (Exception ignored) {}
            
            if (pNode.isListItem()) {
                p.setIndentationLeft(200);
                XWPFRun bullet = p.createRun();
                bullet.setText(pNode.getListSymbol() + " ");
                bullet.setFontFamily("Times New Roman");
                bullet.setFontSize(11);
            }

            for (AstNode child : pNode.getChildren()) {
                renderInlineNode(child, p);
            }
        } else if (node instanceof TableNode) {
            TableNode tNode = (TableNode) node;
            if (tNode.getRows().isEmpty()) return;
            
            int rows = tNode.getRows().size();
            int cols = tNode.getRows().get(0).getCells().size();
            
            org.apache.xmlbeans.XmlCursor cursor = cell.getCTTc().newCursor();
            cursor.toEndToken();
            XWPFTable table = cell.insertNewTbl(cursor);
            cursor.close();
            
            if (table == null) return;
            
            org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTbl ctTbl = table.getCTTbl();
            if (ctTbl.getTblPr() == null) {
                org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr pr = ctTbl.addNewTblPr();
                pr.addNewTblW().setType(org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth.AUTO);
                pr.getTblW().setW(java.math.BigInteger.valueOf(0));
            }
            if (ctTbl.getTblGrid() == null) {
                ctTbl.addNewTblGrid();
                for (int c = 0; c < cols; c++) {
                    ctTbl.getTblGrid().addNewGridCol();
                }
            }

            table.setCellMargins(0, 0, 0, 0);
            String tableWidth = (cols <= 2) ? "40%" : (cols <= 4 ? "70%" : "100%");
            table.setWidth(tableWidth);
            table.setTableAlignment(org.apache.poi.xwpf.usermodel.TableRowAlign.CENTER);

            table.setTopBorder(org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
            table.setBottomBorder(org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
            table.setLeftBorder(org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
            table.setRightBorder(org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
            table.setInsideHBorder(org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
            table.setInsideVBorder(org.apache.poi.xwpf.usermodel.XWPFTable.XWPFBorderType.SINGLE, 4, 0, "000000");
            
            for (int r = 0; r < rows; r++) {
                XWPFTableRow row = table.getRow(r) != null ? table.getRow(r) : table.createRow();
                for (int c = 0; c < cols; c++) {
                    XWPFTableCell tCell = row.getCell(c) != null ? row.getCell(c) : row.createCell();
                    if (c < tNode.getRows().get(r).getCells().size()) {
                        TableNode.TableCellNode cellNode = tNode.getRows().get(r).getCells().get(c);
                        XWPFParagraph p = tCell.getParagraphs().isEmpty() ? tCell.addParagraph() : tCell.getParagraphs().get(0);
                        while (tCell.getParagraphs().size() > 1) {
                            tCell.removeParagraph(1);
                        }
                        while (!p.getRuns().isEmpty()) {
                            p.removeRun(0);
                        }
                        
                        for (AstNode child : cellNode.getContent()) {
                            if (child instanceof ParagraphNode) {
                                ParagraphNode pn = (ParagraphNode) child;
                                for (AstNode pChild : pn.getChildren()) {
                                    renderInlineNode(pChild, p);
                                }
                            } else if (child instanceof TextNode || child instanceof ImageNode || child instanceof FormulaNode) {
                                renderInlineNode(child, p);
                            } else {
                                renderNode(child, tCell);
                            }
                        }
                        p.setAlignment(org.apache.poi.xwpf.usermodel.ParagraphAlignment.CENTER);
                    }
                }
            }
        }
    }

    private static void renderInlineNode(AstNode node, XWPFParagraph p) {
        if (node instanceof TextNode) {
            TextNode tNode = (TextNode) node;
            XWPFRun run = p.createRun();
            run.setText(tNode.getText());
            run.setFontFamily("Times New Roman");
            run.setFontSize(11);
            run.setBold(tNode.isBold());
            run.setItalic(tNode.isItalic());
            if (tNode.isUnderline()) run.setUnderline(UnderlinePatterns.SINGLE);
            if (tNode.isSubscript()) run.setSubscript(VerticalAlign.SUBSCRIPT);
            if (tNode.isSuperscript()) run.setSubscript(VerticalAlign.SUPERSCRIPT);
        } else if (node instanceof ImageNode) {
            ImageNode iNode = (ImageNode) node;
            try {
                byte[] imgData = Base64.getDecoder().decode(iNode.getBase64Data());
                int pictureType = org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_PNG;
                if (iNode.getMimeType() != null) {
                    if (iNode.getMimeType().contains("jpeg") || iNode.getMimeType().contains("jpg")) {
                        pictureType = org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_JPEG;
                    } else if (iNode.getMimeType().contains("gif")) {
                        pictureType = org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_GIF;
                    }
                }
                
                int width = iNode.getWidth();
                int height = iNode.getHeight();
                if (width > 300) {
                    double ratio = 300.0 / width;
                    width = 300;
                    height = (int) (height * ratio);
                }
                if (width == 0) width = 200;
                if (height == 0) height = 200;

                XWPFRun run = p.createRun();
                run.addPicture(new ByteArrayInputStream(imgData), pictureType, "image", Units.toEMU(width), Units.toEMU(height));
            } catch (Exception e) {
                log.warn("Failed to render ImageNode", e);
            }
        } else if (node instanceof FormulaNode) {
            FormulaNode fNode = (FormulaNode) node;
            try {
                String xml = fNode.getRawOmml();
                if (xml != null && !xml.isEmpty()) {
                    XmlObject mathObj = XmlObject.Factory.parse(xml);
                    CTP ctp = p.getCTP();
                    // Inject OMML into paragraph CTP
                    CTOMath math = CTOMath.Factory.parse(mathObj.getDomNode());
                    ctp.addNewOMath().set(math);
                }
            } catch (Exception e) {
                log.warn("Failed to render FormulaNode OMML", e);
                XWPFRun fallback = p.createRun();
                fallback.setText(fNode.getHtmlFallback()); // Basic fallback
            }
        } else if (node instanceof BrNode) {
            p.createRun().addBreak();
        }
    }
}
