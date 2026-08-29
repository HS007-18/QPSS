package com.qpss.document.parser;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class UnitContextResolver {

    private static final Pattern UNIT_PATTERN = Pattern.compile("(?i)^\\s*UNIT\\s*(?:-|:)?\\s*(\\d+).*");
    private static final Pattern FILE_UNIT_PATTERN = Pattern.compile("(?i)unit\\s*[-_]?\\s*(\\d+)");

    private Integer currentUnit;

    public UnitContextResolver(String filename) {
        if (filename != null) {
            Matcher matcher = FILE_UNIT_PATTERN.matcher(filename);
            if (matcher.find()) {
                try {
                    currentUnit = Integer.parseInt(matcher.group(1));
                } catch (NumberFormatException ignored) {
                }
            }
        }
    }

    public void processParagraph(String text) {
        if (text == null || text.trim().isEmpty()) {
            return;
        }

        Matcher matcher = UNIT_PATTERN.matcher(text);
        if (matcher.matches()) {
            try {
                currentUnit = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
    }

    public Integer getCurrentUnit() {
        return currentUnit;
    }
}
