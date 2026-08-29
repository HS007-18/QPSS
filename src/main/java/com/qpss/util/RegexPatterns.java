package com.qpss.util;

import java.util.regex.Pattern;

public final class RegexPatterns {
    private RegexPatterns() {
        // Utility class
    }

    // Pattern to extract subject code like ME3491, CS3351, etc.
    public static final Pattern SUBJECT_CODE_PATTERN = Pattern.compile("\\b(\\d{2}[A-Z]{2,4}\\d{2,3})\\b");
}
