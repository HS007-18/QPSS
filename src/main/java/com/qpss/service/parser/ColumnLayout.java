package com.qpss.service.parser;

import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ColumnLayout {

    public static final int ABSENT = -1;

    public enum Role { SNO, QUESTION, MARKS, RBT, CO, T, TYPE }

    private final Map<Role, Integer> indices = new EnumMap<>(Role.class);

    public ColumnLayout(XWPFTableRow headerRow) {
        List<XWPFTableCell> cells = headerRow.getTableCells();
        for (int i = 0; i < cells.size(); i++) {
            Role role = detectRole(clean(cells.get(i).getText()));
            if (role != null && !indices.containsKey(role)) {
                indices.put(role, i);
            }
        }
    }

    public boolean isValid() {
        return has(Role.SNO) && has(Role.QUESTION) && has(Role.MARKS);
    }

    public boolean has(Role role) {
        return indices.containsKey(role);
    }

    public int indexOf(Role role) {
        return indices.getOrDefault(role, ABSENT);
    }

    private static Role detectRole(String text) {
        if (text.isEmpty()) {
            return null;
        }
        if (text.contains("mark")) {
            return Role.MARKS;
        }
        if (text.equals("m")) {
            return Role.MARKS;
        }
        if (text.contains("question") || text.contains("desc")) {
            return Role.QUESTION;
        }
        if (text.contains("part")) {
            return Role.QUESTION;
        }
        if (text.contains("rbt")) {
            return Role.RBT;
        }
        if (text.equals("co")) {
            return Role.CO;
        }
        if (text.contains("th") || text.contains("pr") || text.contains("de")) {
            return Role.TYPE;
        }
        if (text.equals("iii") || text.equals("iiihalf")) {
            return Role.T;
        }
        if (text.equals("sno") || text.equals("qno") || text.contains("serial")
                || (text.endsWith("no") && text.length() <= 4)) {
            return Role.SNO;
        }
        return null;
    }

    private static String clean(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
