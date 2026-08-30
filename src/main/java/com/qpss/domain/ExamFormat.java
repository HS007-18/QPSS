package com.qpss.domain;

public enum ExamFormat {
    FORMAT_1,
    FORMAT_2,
    FORMAT_3;

    public static ExamFormat from(String value) {
        if (value == null || value.trim().isEmpty()) {
            return FORMAT_1; // default
        }
        try {
            return ExamFormat.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported format: " + value);
        }
    }
}
