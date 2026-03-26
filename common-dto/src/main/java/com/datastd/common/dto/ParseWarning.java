package com.datastd.common.dto;

/**
 * Represents a warning generated when a row in a file could not be parsed.
 */
public class ParseWarning {

    private int rowIndex;      // 1-based row number in the original file
    private String reason;     // human-readable reason for the warning
    private String rawValue;   // raw cell/row value that failed (truncated to 200 chars)

    public ParseWarning() {}

    public ParseWarning(int rowIndex, String reason, String rawValue) {
        this.rowIndex = rowIndex;
        this.reason = reason;
        this.rawValue = truncate(rawValue, 200);
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() > maxLength ? value.substring(0, maxLength) + "..." : value;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRawValue() {
        return rawValue;
    }

    public void setRawValue(String rawValue) {
        this.rawValue = rawValue;
    }
}

