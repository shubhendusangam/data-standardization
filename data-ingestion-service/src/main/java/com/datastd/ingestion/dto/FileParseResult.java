package com.datastd.ingestion.dto;

import com.datastd.common.dto.ParseWarning;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Encapsulates the result of parsing a file: the successfully parsed records,
 * any parse warnings for skipped/coerced rows, and metadata about truncation.
 */
public class FileParseResult {

    private static final int MAX_WARNINGS = 100;

    private final List<Map<String, String>> records;
    private final List<ParseWarning> warnings;
    private final int totalSkippedCount;
    private final boolean warningsTruncated;

    public FileParseResult(List<Map<String, String>> records, List<ParseWarning> allWarnings) {
        this.records = records;
        this.totalSkippedCount = allWarnings.size();
        this.warningsTruncated = allWarnings.size() > MAX_WARNINGS;
        this.warnings = this.warningsTruncated
                ? new ArrayList<>(allWarnings.subList(0, MAX_WARNINGS))
                : new ArrayList<>(allWarnings);
    }

    public List<Map<String, String>> getRecords() {
        return records;
    }

    public List<ParseWarning> getWarnings() {
        return warnings;
    }

    public int getParsedRowCount() {
        return records.size();
    }

    public int getSkippedRowCount() {
        return totalSkippedCount;
    }

    public boolean isWarningsTruncated() {
        return warningsTruncated;
    }
}

