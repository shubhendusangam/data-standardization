package com.datastd.quality.engine;

import com.datastd.common.dto.ColumnReport;
import com.datastd.common.dto.InferredType;
import com.datastd.quality.dto.SuggestedRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Heuristic engine that suggests validation rules based on column profiles.
 * Does NOT save rules — returns suggestions for the client to review and accept.
 */
@Component
public class RuleSuggestionEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleSuggestionEngine.class);

    private static final Set<String> NUMERIC_COLUMN_HINTS = Set.of(
            "age", "salary", "amount", "quantity", "price", "total", "count", "score", "rate");

    private static final Set<String> ID_COLUMN_HINTS = Set.of(
            "id", "uuid", "key", "code", "identifier");

    private final ColumnProfiler columnProfiler;

    public RuleSuggestionEngine(ColumnProfiler columnProfiler) {
        this.columnProfiler = columnProfiler;
    }

    /**
     * Generate rule suggestions for a list of column profiles.
     */
    public List<SuggestedRule> suggest(List<ColumnReport> columnProfiles) {
        List<SuggestedRule> suggestions = new ArrayList<>();

        for (ColumnReport col : columnProfiles) {
            String name = col.getColumnName().toLowerCase();

            // Email column
            if (name.contains("email")) {
                suggestions.add(new SuggestedRule(
                        "REGEX_MATCH", col.getColumnName(),
                        "{\"pattern\":\"^[\\\\w.+]+@[\\\\w.]+\\\\.[a-z]{2,}$\",\"maxFailRatePct\":0}",
                        "ERROR",
                        String.format("Column '%s' appears to contain email addresses based on its name", col.getColumnName()),
                        "HIGH"));
            }

            // Phone column
            if (name.contains("phone") || name.contains("tel") || name.contains("mobile")) {
                suggestions.add(new SuggestedRule(
                        "REGEX_MATCH", col.getColumnName(),
                        "{\"pattern\":\"^[+\\\\d\\\\s()-]{7,20}$\",\"maxFailRatePct\":5}",
                        "WARNING",
                        String.format("Column '%s' appears to contain phone numbers based on its name", col.getColumnName()),
                        "HIGH"));
            }

            // Name column
            if (name.contains("name") && !name.contains("file")) {
                suggestions.add(new SuggestedRule(
                        "NOT_EMPTY", col.getColumnName(),
                        "{\"maxNullRatePct\":0}",
                        "ERROR",
                        String.format("Column '%s' appears to be a name field — should not be empty", col.getColumnName()),
                        "HIGH"));
            }

            // Numeric column with range
            if ((col.getInferredType() == InferredType.INTEGER || col.getInferredType() == InferredType.DECIMAL)
                    && NUMERIC_COLUMN_HINTS.stream().anyMatch(name::contains)) {
                String params;
                if (col.getMin() != null && col.getMax() != null) {
                    params = String.format("{\"min\":%.0f,\"max\":%.0f,\"maxFailRatePct\":0}",
                            col.getMin(), col.getMax());
                } else {
                    params = "{\"min\":0,\"max\":999999,\"maxFailRatePct\":0}";
                }
                suggestions.add(new SuggestedRule(
                        "NUMERIC_RANGE", col.getColumnName(),
                        params, "ERROR",
                        String.format("Column '%s' is numeric (inferred: %s) with name suggesting a bounded value",
                                col.getColumnName(), col.getInferredType()),
                        "MEDIUM"));
            }

            // Date column — suggest regex for the most common pattern
            if (col.getInferredType() == InferredType.DATE && col.getSampleValues() != null && !col.getSampleValues().isEmpty()) {
                String detectedPattern = columnProfiler.detectDatePattern(col.getSampleValues());
                String regexPattern = datePatternToRegex(detectedPattern);
                suggestions.add(new SuggestedRule(
                        "REGEX_MATCH", col.getColumnName(),
                        String.format("{\"pattern\":\"%s\",\"maxFailRatePct\":0}", regexPattern),
                        "WARNING",
                        String.format("Column '%s' contains dates — most common format: %s",
                                col.getColumnName(), detectedPattern),
                        "MEDIUM"));
            }

            // Unique ID column
            if (col.getUniqueRatePct() == 100.0 && ID_COLUMN_HINTS.stream().anyMatch(name::contains)) {
                suggestions.add(new SuggestedRule(
                        "UNIQUE", col.getColumnName(),
                        "{\"maxDuplicateRatePct\":0}",
                        "ERROR",
                        String.format("Column '%s' has 100%% unique values and appears to be an identifier", col.getColumnName()),
                        "HIGH"));
            }

            // NOT_NULL for any column with nulls
            if (col.getNullRatePct() > 0) {
                double threshold = Math.ceil(col.getNullRatePct() + 2);
                suggestions.add(new SuggestedRule(
                        "NOT_NULL", col.getColumnName(),
                        String.format("{\"maxNullRatePct\":%.0f}", threshold),
                        "WARNING",
                        String.format("Column '%s' has %.1f%% null values — suggest a null check with %.0f%% threshold",
                                col.getColumnName(), col.getNullRatePct(), threshold),
                        "LOW"));
            }

            // Gender / status / category — small distinct set → ALLOWED_VALUES
            if (col.getUniqueCount() > 0 && col.getUniqueCount() <= 10 && col.getTotalCount() > 5
                    && (name.contains("gender") || name.contains("status") || name.contains("type")
                        || name.contains("category"))) {
                List<String> allowedValues = col.getSampleValues();
                if (col.getTopValues() != null && !col.getTopValues().isEmpty()) {
                    allowedValues = col.getTopValues().stream()
                            .map(ColumnReport.TopValue::getValue)
                            .toList();
                }
                String valuesJson = "[" + allowedValues.stream()
                        .map(v -> "\"" + v.replace("\"", "\\\"") + "\"")
                        .reduce((a, b) -> a + "," + b).orElse("") + "]";
                suggestions.add(new SuggestedRule(
                        "ALLOWED_VALUES", col.getColumnName(),
                        String.format("{\"values\":%s,\"maxFailRatePct\":0}", valuesJson),
                        "WARNING",
                        String.format("Column '%s' has only %d distinct values — suggest an allowed values check",
                                col.getColumnName(), col.getUniqueCount()),
                        "MEDIUM"));
            }
        }

        log.info("Generated {} rule suggestions for {} columns", suggestions.size(), columnProfiles.size());
        return suggestions;
    }

    private String datePatternToRegex(String datePattern) {
        return switch (datePattern) {
            case "MM/dd/yyyy" -> "^\\\\d{2}/\\\\d{2}/\\\\d{4}$";
            case "dd-MM-yyyy" -> "^\\\\d{2}-\\\\d{2}-\\\\d{4}$";
            default -> "^\\\\d{4}-\\\\d{2}-\\\\d{2}$"; // yyyy-MM-dd
        };
    }
}

