package com.qpss.common.domain;

public enum PaperSection {

    SECTION_A,
    SECTION_B;

    public static PaperSection from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Paper section is required");
        }
        try {
            return PaperSection.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported paper section: " + value);
        }
    }
}
