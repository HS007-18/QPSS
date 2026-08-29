package com.qpss.domain;
public enum ExamType {

    INTERNAL_1,
    INTERNAL_2,
    SEMESTER;

    public static ExamType from(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Exam type is required");
        }
        try {
            return ExamType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported exam type: " + value);
        }
    }

    public boolean isCrossHalf() {
        return this == SEMESTER;
    }
}
