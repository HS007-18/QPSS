package com.qpss.questionbank.parser;
import com.qpss.questionbank.model.HeaderMetadata;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import java.util.ArrayList;
import java.util.List;
public class HeaderMetadataExtractor {
    public HeaderMetadata extract(XWPFDocument document) {
        HeaderMetadata metadata = HeaderMetadata.builder().courseOutcomes(new ArrayList<>()).build();
        if (document.getTables().isEmpty()) return metadata;
        for(XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                List<XWPFTableCell> cells = row.getTableCells();
                if (cells.isEmpty()) continue;
                String rowText = cells.get(0).getText().trim();
                if (rowText.contains("Q.No") || rowText.contains("PART - A") || rowText.contains("PART A")) break;
                String lowerRow = rowText.toLowerCase();
                if (lowerRow.contains("institute") || lowerRow.contains("college") || lowerRow.contains("university")) {
                    int parenIdx = rowText.indexOf("(");
                    if(parenIdx > 0) {
                        metadata.setInstitutionName(rowText.substring(0, parenIdx).trim());
                        metadata.setTagline(rowText.substring(parenIdx).trim());
                    } else {
                        metadata.setInstitutionName(rowText);
                    }
                } else if (lowerRow.contains("examination") || lowerRow.contains("assessment")) {
                    metadata.setExamTitle(rowText);
                } else if (lowerRow.contains("semester")) {
                    metadata.setSemester(rowText);
                } else if (rowText.matches(".*\\d{2}[A-Z]{3}\\d{3}.*") || rowText.contains(" - ")) {
                    if (!lowerRow.contains("common to") && !lowerRow.contains("note")) metadata.setSubjectCodeTitle(rowText);
                } else if ((rowText.contains("CSE") || rowText.contains("ECE") || rowText.contains("EEE") || rowText.contains("ME") || rowText.contains("IT") || rowText.contains("CIVIL") || rowText.contains("AI")) && !lowerRow.contains("common to")) {
                    metadata.setDepartment(rowText);
                } else if (lowerRow.contains("common to")) {
                    metadata.setCommonTo(rowText);
                } else if (lowerRow.contains("note:")) {
                    metadata.setNotes(rowText);
                } else if (lowerRow.contains("regulation")) {
                    metadata.setRegulation(rowText);
                }
                if (cells.size() >= 2) {
                    String c0 = cells.get(0).getText().trim();
                    String c1 = cells.get(1).getText().trim();
                    if (c0.toUpperCase().startsWith("CO") && !c0.toUpperCase().startsWith("COURSE") && !c1.isEmpty()) {
                        metadata.getCourseOutcomes().add(new HeaderMetadata.CourseOutcome(c0.replaceAll("[^a-zA-Z0-9]", ""), c1));
                    }
                } else if (rowText.toUpperCase().startsWith("CO") && !rowText.toUpperCase().startsWith("COURSE")) {
                    int colonIdx = rowText.indexOf(":");
                    if (colonIdx > 0) {
                        metadata.getCourseOutcomes().add(new HeaderMetadata.CourseOutcome(rowText.substring(0, colonIdx).trim().replaceAll("[^a-zA-Z0-9]", ""), rowText.substring(colonIdx + 1).trim()));
                    }
                }
            }
        }
        return metadata;
    }
}