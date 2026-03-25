package com.datastd.quality.engine;

import com.datastd.common.dto.ColumnReport;
import com.datastd.common.dto.InferredType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Profiles all columns in a dataset, computing rich statistics per column.
 * Run as part of every POST /api/quality/validate before rule checks.
 */
@Component
public class ColumnProfiler {

    private static final Logger log = LoggerFactory.getLogger(ColumnProfiler.class);

    private static final Set<String> BOOLEAN_VALUES = Set.of(
            "true", "false", "yes", "no", "1", "0", "y", "n");

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("MM/dd/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ISO_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    /**
     * Profile all columns in the dataset.
     */
    public List<ColumnReport> profileDataset(List<Map<String, Object>> records) {
        Set<String> allColumns = new LinkedHashSet<>();
        for (Map<String, Object> record : records) {
            allColumns.addAll(record.keySet());
        }
        return allColumns.stream()
                .map(col -> profileColumn(col, records))
                .collect(Collectors.toList());
    }

    /**
     * Profile a single column: counts, rates, type inference,
     * string lengths, numeric stats, top values, sample values.
     */
    public ColumnReport profileColumn(String columnName, List<Map<String, Object>> records) {
        int total = records.size();
        int nullCount = 0;
        Map<String, Integer> valueCounts = new LinkedHashMap<>();
        List<String> nonNullStringValues = new ArrayList<>();

        for (Map<String, Object> record : records) {
            Object value = record.get(columnName);
            if (value == null) {
                nullCount++;
            } else {
                String s = value.toString();
                nonNullStringValues.add(s);
                valueCounts.merge(s, 1, Integer::sum);
            }
        }

        int uniqueCount = valueCounts.size();
        double nullRatePct = total == 0 ? 0 : round((double) nullCount / total * 100);
        double uniqueRatePct = total == 0 ? 0 : round((double) uniqueCount / total * 100);

        // Sample values: first 5 distinct non-null
        List<String> sampleValues = valueCounts.keySet().stream().limit(5).collect(Collectors.toList());

        // Inferred type (enhanced: >90% threshold, <70% → MIXED)
        InferredType inferredType = inferType(nonNullStringValues, records, columnName);

        // Top 10 most frequent values
        List<ColumnReport.TopValue> topValues = valueCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(e -> new ColumnReport.TopValue(e.getKey(), e.getValue(),
                        total == 0 ? 0 : round((double) e.getValue() / total * 100)))
                .collect(Collectors.toList());

        // String length stats (for all non-null values)
        Integer minLength = null;
        Integer maxLength = null;
        Double avgLength = null;
        if (!nonNullStringValues.isEmpty()) {
            int minL = Integer.MAX_VALUE, maxL = 0;
            long sumL = 0;
            for (String s : nonNullStringValues) {
                int len = s.length();
                minL = Math.min(minL, len);
                maxL = Math.max(maxL, len);
                sumL += len;
            }
            minLength = minL;
            maxLength = maxL;
            avgLength = round((double) sumL / nonNullStringValues.size());
        }

        // Numeric stats (only if inferred type is numeric)
        Double min = null, max = null, mean = null, stddev = null;
        if (inferredType == InferredType.INTEGER || inferredType == InferredType.DECIMAL) {
            List<Double> numbers = new ArrayList<>();
            for (String s : nonNullStringValues) {
                try {
                    numbers.add(Double.parseDouble(s));
                } catch (NumberFormatException ignored) {}
            }
            if (!numbers.isEmpty()) {
                double sum = 0;
                double minVal = Double.MAX_VALUE, maxVal = -Double.MAX_VALUE;
                for (double v : numbers) {
                    sum += v;
                    minVal = Math.min(minVal, v);
                    maxVal = Math.max(maxVal, v);
                }
                min = round(minVal);
                max = round(maxVal);
                mean = round(sum / numbers.size());

                // Standard deviation
                double meanVal = sum / numbers.size();
                double sumSqDiff = 0;
                for (double v : numbers) {
                    sumSqDiff += (v - meanVal) * (v - meanVal);
                }
                stddev = round(Math.sqrt(sumSqDiff / numbers.size()));
            }
        }

        // Build ColumnReport
        ColumnReport cr = new ColumnReport();
        cr.setColumnName(columnName);
        cr.setTotalCount(total);
        cr.setNullCount(nullCount);
        cr.setNullRatePct(nullRatePct);
        cr.setUniqueCount(uniqueCount);
        cr.setUniqueRatePct(uniqueRatePct);
        cr.setSampleValues(sampleValues);
        cr.setInferredType(inferredType);
        cr.setTopValues(topValues);
        cr.setMinLength(minLength);
        cr.setMaxLength(maxLength);
        cr.setAvgLength(avgLength);
        cr.setMin(min);
        cr.setMax(max);
        cr.setMean(mean);
        cr.setStddev(stddev);
        return cr;
    }

    // ── Type Inference (enhanced: >90% threshold, <70% → MIXED) ─────

    /**
     * Infer the type of a column using threshold-based logic:
     * - >90% of non-null values parse as type T → return T
     * - <70% agree on any single type → MIXED
     * - otherwise → the majority type
     */
    public InferredType inferType(List<String> nonNullValues, List<Map<String, Object>> records, String columnName) {
        if (nonNullValues.isEmpty()) return InferredType.UNKNOWN;

        int total = nonNullValues.size();
        int intCount = 0, decCount = 0, boolCount = 0, dateCount = 0;

        for (Map<String, Object> record : records) {
            Object value = record.get(columnName);
            if (value == null) continue;

            // Check Java type first
            if (value instanceof Boolean) { boolCount++; continue; }
            if (value instanceof Integer || value instanceof Long) { intCount++; continue; }
            if (value instanceof Float || value instanceof Double || value instanceof BigDecimal) { decCount++; continue; }

            String s = value.toString();
            if (isInteger(s)) intCount++;
            else if (isDecimal(s)) decCount++;
            else if (isBoolean(s)) boolCount++;
            else if (isDate(s)) dateCount++;
            // else: string — counted implicitly
        }

        // Compute string count as the remainder
        int stringCount = total - intCount - decCount - boolCount - dateCount;

        // Find the dominant type
        Map<InferredType, Integer> counts = new LinkedHashMap<>();
        if (intCount > 0) counts.put(InferredType.INTEGER, intCount);
        if (decCount > 0) counts.put(InferredType.DECIMAL, decCount);
        if (boolCount > 0) counts.put(InferredType.BOOLEAN, boolCount);
        if (dateCount > 0) counts.put(InferredType.DATE, dateCount);
        if (stringCount > 0) counts.put(InferredType.STRING, stringCount);

        if (counts.isEmpty()) return InferredType.UNKNOWN;
        if (counts.size() == 1) return counts.keySet().iterator().next();

        // Find the type with the highest count
        Map.Entry<InferredType, Integer> best = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue()).orElse(null);
        if (best == null) return InferredType.UNKNOWN;

        double bestPct = (double) best.getValue() / total * 100;

        if (bestPct >= 90) return best.getKey();
        if (bestPct < 70) return InferredType.MIXED;
        return best.getKey();
    }

    // ── Parsing helpers ─────────────────────────────────────────────

    public static boolean isInteger(String s) {
        try { Long.parseLong(s); return true; } catch (NumberFormatException e) { return false; }
    }

    public static boolean isDecimal(String s) {
        if (isInteger(s)) return false; // integers are not decimals for inference
        try { Double.parseDouble(s); return true; } catch (NumberFormatException e) { return false; }
    }

    public static boolean isBoolean(String s) {
        return BOOLEAN_VALUES.contains(s.toLowerCase());
    }

    public static boolean isDate(String s) {
        for (DateTimeFormatter fmt : DATE_FORMATTERS) {
            try {
                LocalDate.parse(s, fmt);
                return true;
            } catch (Exception ignored) {}
            try {
                LocalDateTime.parse(s, fmt);
                return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    /**
     * Detect the most common date pattern in a list of date strings.
     */
    public String detectDatePattern(List<String> dateValues) {
        String[] patterns = {"yyyy-MM-dd", "MM/dd/yyyy", "dd-MM-yyyy"};
        int bestCount = 0;
        String bestPattern = "yyyy-MM-dd";

        for (String pattern : patterns) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern(pattern);
            int count = 0;
            for (String val : dateValues) {
                try {
                    LocalDate.parse(val, fmt);
                    count++;
                } catch (Exception ignored) {}
            }
            if (count > bestCount) {
                bestCount = count;
                bestPattern = pattern;
            }
        }
        return bestPattern;
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}

