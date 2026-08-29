package com.qpss.util;

public final class LayoutConstants {

    private LayoutConstants() {
        // Utility class
    }

    // A4 Portrait Page margins (in twips, 1/20 of a point)
    public static final int PAGE_MARGIN_TOP = 720;
    public static final int PAGE_MARGIN_BOTTOM = 720;
    public static final int PAGE_MARGIN_LEFT = 720;
    public static final int PAGE_MARGIN_RIGHT = 720;

    // Master Table Column Widths
    public static final String[] MASTER_TABLE_COLUMN_WIDTHS = {
        "600",   // Q. No.
        "7800",  // Questions
        "400",   // Marks
        "500",   // CO
        "500"    // RBT
    };
}
