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
        tableLoop:
        for (XWPFTable table : document.getTables()) {
            for (XWPFTableRow row : table.getRows()) {
                List<XWPFTableCell> cells = row.getTableCells();
                if (cells.isEmpty()) continue;
                String rowText = cells.get(0).getText().trim();
                if (rowText.contains("Q.No") || rowText.contains("PART - A") || rowText.contains("PART A")) break tableLoop;
                String lowerRow = rowText.toLowerCase();
                if (lowerRow.contains("institute") || lowerRow.contains("college") || lowerRow.contains("university")) {
                    int parenIdx = rowText.indexOf("(");
                    if (parenIdx > 0) {
                        metadata.setInstitutionName(rowText.substring(0, parenIdx).trim());
                        metadata.setTagline(rowText.substring(parenIdx).trim());
                    } else {
                        metadata.setInstitutionName(rowText);
                    }
                } else if (lowerRow.contains("examination") || lowerRow.contains("assessment")) {
                    metadata.setExamTitle(rowText);
                } else if (lowerRow.contains("semester")) {
                    metadata.setSemester(rowText);
                } else if (lowerRow.contains("common to")) {
                    metadata.setCommonTo(rowText);
                } else if (lowerRow.contains("note:") || lowerRow.contains("note :")) {
                    metadata.setNotes(rowText);
                } else if (lowerRow.contains("regulation")) {
                    metadata.setRegulation(rowText);
                } else if (rowText.matches(".*\\d{2}[A-Z]{3}\\d{3}.*") || (rowText.contains(" - ") && !lowerRow.contains("examination"))) {
                    metadata.setSubjectCodeTitle(rowText);
                } else if (!isCoRow(rowText) && !rowText.isEmpty() && metadata.getSemester() != null && metadata.getDepartment() == null && metadata.getSubjectCodeTitle() == null) {
                    metadata.setDepartment(rowText);
                } else if (lowerRow.contains("department") || lowerRow.contains("branch") || lowerRow.contains("engineering") || lowerRow.contains("technology")) {
                    if (metadata.getDepartment() == null) metadata.setDepartment(rowText);
                }
                if (cells.size() >= 2) {
                    String c0 = cells.get(0).getText().trim();
                    String c1 = cells.get(1).getText().trim();
                    if (isCoRow(c0) && !c1.isEmpty()) {
                        metadata.getCourseOutcomes().add(new HeaderMetadata.CourseOutcome(c0.replaceAll("[^a-zA-Z0-9]", ""), c1));
                    }
                } else if (isCoRow(rowText)) {
                    int colonIdx = rowText.indexOf(":");
                    if (colonIdx > 0) {
                        metadata.getCourseOutcomes().add(new HeaderMetadata.CourseOutcome(rowText.substring(0, colonIdx).trim().replaceAll("[^a-zA-Z0-9]", ""), rowText.substring(colonIdx + 1).trim()));
                    }
                }
            }
        }
        return metadata;
    }
    private boolean isCoRow(String text) {
        return text != null && text.trim().toUpperCase().matches("CO\\d+.*");
    }
}