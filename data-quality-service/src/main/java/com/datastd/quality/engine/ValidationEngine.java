package com.datastd.quality.engine;

import com.datastd.common.dto.*;
import com.datastd.quality.entity.Severity;
import com.datastd.quality.entity.ValidationRule;
import com.datastd.quality.entity.ValidationType;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Core validation engine that evaluates a list of records against validation rules
 * and produces a {@link QualityReport}.
 */
@Component
public class ValidationEngine {

    private static final Logger log = LoggerFactory.getLogger(ValidationEngine.class);
    private static final int ERROR_PENALTY = 20;
    private static final int WARNING_PENALTY = 5;

    private final ObjectMapper objectMapper;
    private final ColumnProfiler columnProfiler;

    public ValidationEngine(ObjectMapper objectMapper, ColumnProfiler columnProfiler) {
        this.objectMapper = objectMapper;
        this.columnProfiler = columnProfiler;
    }

    /**
     * Validate records against the given rules and produce a QualityReport.
     */
    public QualityReport validate(UUID datasetId, List<Map<String, Object>> records, List<ValidationRule> rules) {
        log.info("Starting validation: datasetId={}, records={}, rules={}", datasetId, records.size(), rules.size());

        int totalRecords = records.size();

        // 1. Compute column-level stats via ColumnProfiler
        List<ColumnReport> columnReports = columnProfiler.profileDataset(records);

        Set<String> allColumns = new LinkedHashSet<>();
        for (Map<String, Object> record : records) {
            allColumns.addAll(record.keySet());
        }

        // 2. Compute duplicate count using SHA-256 row hashing
        int duplicateCount = computeDuplicateCount(records);

        // 3. Run each rule and collect results
        List<ValidationRuleResult> ruleResults = new ArrayList<>();
        for (ValidationRule rule : rules) {
            if (!rule.isActive()) continue;

            // Determine which columns this rule applies to
            List<String> targetColumns;
            if ("*".equals(rule.getColumnName())) {
                targetColumns = new ArrayList<>(allColumns);
            } else {
                targetColumns = List.of(rule.getColumnName());
            }

            for (String column : targetColumns) {
                ValidationRuleResult result = evaluateRule(rule, column, records);
                ruleResults.add(result);
            }
        }

        // 4. Compute quality score & overall status
        int penaltySum = 0;
        boolean hasErrorViolation = false;
        boolean hasWarningViolation = false;

        for (ValidationRuleResult r : ruleResults) {
            if (!r.isPassed()) {
                if ("ERROR".equals(r.getSeverity())) {
                    penaltySum += ERROR_PENALTY;
                    hasErrorViolation = true;
                } else {
                    penaltySum += WARNING_PENALTY;
                    hasWarningViolation = true;
                }
            }
        }

        int qualityScore = Math.max(0, 100 - penaltySum);

        OverallStatus overallStatus;
        if (hasErrorViolation) {
            overallStatus = OverallStatus.FAIL;
        } else if (hasWarningViolation) {
            overallStatus = OverallStatus.WARN;
        } else {
            overallStatus = OverallStatus.PASS;
        }

        // 5. Build report
        QualityReport report = new QualityReport();
        report.setDatasetId(datasetId);
        report.setOverallStatus(overallStatus);
        report.setQualityScore(qualityScore);
        report.setTotalRecords(totalRecords);
        report.setDuplicateCount(duplicateCount);
        report.setColumnReports(columnReports);
        report.setRuleResults(ruleResults);
        report.setEvaluatedAt(Instant.now());

        log.info("Validation complete: datasetId={}, status={}, score={}, duplicates={}",
                datasetId, overallStatus, qualityScore, duplicateCount);
        return report;
    }


    // ── Duplicate Detection ─────────────────────────────────────────

    private int computeDuplicateCount(List<Map<String, Object>> records) {
        Map<String, Integer> hashCounts = new HashMap<>();
        for (Map<String, Object> record : records) {
            String hash = hashRow(record);
            hashCounts.merge(hash, 1, Integer::sum);
        }
        int duplicates = 0;
        for (int count : hashCounts.values()) {
            if (count > 1) {
                duplicates += (count - 1);
            }
        }
        return duplicates;
    }

    private String hashRow(Map<String, Object> record) {
        try {
            // Sort keys for deterministic ordering
            String rowString = new TreeMap<>(record).toString();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rowString.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ── Per-Rule Evaluation ─────────────────────────────────────────

    private ValidationRuleResult evaluateRule(ValidationRule rule, String columnName, List<Map<String, Object>> records) {
        Map<String, Object> params = parseParams(rule.getParams());

        ValidationRuleResult result = new ValidationRuleResult();
        result.setRuleId(rule.getId());
        result.setRuleName(rule.getName());
        result.setColumnName(columnName);
        result.setValidationType(rule.getValidationType().name());
        result.setSeverity(rule.getSeverity().name());

        int total = records.size();
        if (total == 0) {
            result.setPassed(true);
            result.setFailRatePct(0);
            result.setFailCount(0);
            result.setMessage(columnName + ": no records to validate");
            return result;
        }

        switch (rule.getValidationType()) {
            case NOT_NULL -> evaluateNotNull(result, columnName, records, params);
            case NOT_EMPTY -> evaluateNotEmpty(result, columnName, records, params);
            case REGEX_MATCH -> evaluateRegexMatch(result, columnName, records, params);
            case ALLOWED_VALUES -> evaluateAllowedValues(result, columnName, records, params);
            case NUMERIC_RANGE -> evaluateNumericRange(result, columnName, records, params);
            case MIN_LENGTH -> evaluateMinLength(result, columnName, records, params);
            case MAX_LENGTH -> evaluateMaxLength(result, columnName, records, params);
            case UNIQUE -> evaluateUnique(result, columnName, records, params);
            case CUSTOM_SQL -> {
                result.setPassed(true);
                result.setFailRatePct(0);
                result.setFailCount(0);
                result.setMessage(columnName + ": CUSTOM_SQL not yet implemented");
            }
        }

        return result;
    }

    private void evaluateNotNull(ValidationRuleResult result, String column,
                                  List<Map<String, Object>> records, Map<String, Object> params) {
        int total = records.size();
        long nullCount = records.stream().filter(r -> r.get(column) == null).count();
        double nullRate = round((double) nullCount / total * 100);
        double maxRate = getDouble(params, "maxNullRatePct", 0.0);

        result.setFailCount((int) nullCount);
        result.setFailRatePct(nullRate);
        result.setPassed(nullRate <= maxRate);
        result.setMessage(String.format("%s: %.1f%% null values (threshold: %.1f%%)", column, nullRate, maxRate));
    }

    private void evaluateNotEmpty(ValidationRuleResult result, String column,
                                   List<Map<String, Object>> records, Map<String, Object> params) {
        int total = records.size();
        long emptyCount = records.stream().filter(r -> {
            Object val = r.get(column);
            return val == null || val.toString().isBlank();
        }).count();
        double emptyRate = round((double) emptyCount / total * 100);
        double maxRate = getDouble(params, "maxNullRatePct", 0.0);

        result.setFailCount((int) emptyCount);
        result.setFailRatePct(emptyRate);
        result.setPassed(emptyRate <= maxRate);
        result.setMessage(String.format("%s: %.1f%% blank/null values (threshold: %.1f%%)", column, emptyRate, maxRate));
    }

    private void evaluateRegexMatch(ValidationRuleResult result, String column,
                                     List<Map<String, Object>> records, Map<String, Object> params) {
        int total = records.size();
        String patternStr = (String) params.getOrDefault("pattern", ".*");
        double maxFailRate = getDouble(params, "maxFailRatePct", 0.0);
        Pattern pattern = Pattern.compile(patternStr);

        long failCount = records.stream().filter(r -> {
            Object val = r.get(column);
            if (val == null) return true;
            return !pattern.matcher(val.toString()).matches();
        }).count();

        double failRate = round((double) failCount / total * 100);
        result.setFailCount((int) failCount);
        result.setFailRatePct(failRate);
        result.setPassed(failRate <= maxFailRate);
        result.setMessage(String.format("%s: %.1f%% of values fail regex match (threshold: %.1f%%)", column, failRate, maxFailRate));
    }

    private void evaluateAllowedValues(ValidationRuleResult result, String column,
                                        List<Map<String, Object>> records, Map<String, Object> params) {
        int total = records.size();
        @SuppressWarnings("unchecked")
        List<String> allowed = (List<String>) params.getOrDefault("values", List.of());
        Set<String> allowedSet = new HashSet<>(allowed);
        double maxFailRate = getDouble(params, "maxFailRatePct", 0.0);

        long failCount = records.stream().filter(r -> {
            Object val = r.get(column);
            if (val == null) return true;
            return !allowedSet.contains(val.toString());
        }).count();

        double failRate = round((double) failCount / total * 100);
        result.setFailCount((int) failCount);
        result.setFailRatePct(failRate);
        result.setPassed(failRate <= maxFailRate);
        result.setMessage(String.format("%s: %.1f%% of values not in allowed set (threshold: %.1f%%)", column, failRate, maxFailRate));
    }

    private void evaluateNumericRange(ValidationRuleResult result, String column,
                                       List<Map<String, Object>> records, Map<String, Object> params) {
        int total = records.size();
        double min = getDouble(params, "min", Double.NEGATIVE_INFINITY);
        double max = getDouble(params, "max", Double.POSITIVE_INFINITY);
        double maxFailRate = getDouble(params, "maxFailRatePct", 0.0);

        long failCount = records.stream().filter(r -> {
            Object val = r.get(column);
            if (val == null) return true;
            try {
                double num = Double.parseDouble(val.toString());
                return num < min || num > max;
            } catch (NumberFormatException e) {
                return true;
            }
        }).count();

        double failRate = round((double) failCount / total * 100);
        result.setFailCount((int) failCount);
        result.setFailRatePct(failRate);
        result.setPassed(failRate <= maxFailRate);
        result.setMessage(String.format("%s: %.1f%% of values outside range [%.1f, %.1f] (threshold: %.1f%%)",
                column, failRate, min, max, maxFailRate));
    }

    private void evaluateMinLength(ValidationRuleResult result, String column,
                                    List<Map<String, Object>> records, Map<String, Object> params) {
        int total = records.size();
        int minLen = getInt(params, "length", 0);

        long failCount = records.stream().filter(r -> {
            Object val = r.get(column);
            if (val == null) return true;
            return val.toString().length() < minLen;
        }).count();

        double failRate = round((double) failCount / total * 100);
        result.setFailCount((int) failCount);
        result.setFailRatePct(failRate);
        result.setPassed(failCount == 0);
        result.setMessage(String.format("%s: %d values shorter than %d chars", column, failCount, minLen));
    }

    private void evaluateMaxLength(ValidationRuleResult result, String column,
                                    List<Map<String, Object>> records, Map<String, Object> params) {
        int total = records.size();
        int maxLen = getInt(params, "length", Integer.MAX_VALUE);

        long failCount = records.stream().filter(r -> {
            Object val = r.get(column);
            if (val == null) return false;
            return val.toString().length() > maxLen;
        }).count();

        double failRate = round((double) failCount / total * 100);
        result.setFailCount((int) failCount);
        result.setFailRatePct(failRate);
        result.setPassed(failCount == 0);
        result.setMessage(String.format("%s: %d values longer than %d chars", column, failCount, maxLen));
    }

    private void evaluateUnique(ValidationRuleResult result, String column,
                                 List<Map<String, Object>> records, Map<String, Object> params) {
        int total = records.size();
        double maxDupRate = getDouble(params, "maxDuplicateRatePct", 0.0);

        Map<String, Integer> valueCounts = new HashMap<>();
        for (Map<String, Object> record : records) {
            Object val = record.get(column);
            String key = val == null ? "__NULL__" : val.toString();
            valueCounts.merge(key, 1, Integer::sum);
        }

        long duplicateCount = 0;
        for (int count : valueCounts.values()) {
            if (count > 1) duplicateCount += (count - 1);
        }

        double dupRate = total == 0 ? 0 : round((double) duplicateCount / total * 100);
        result.setFailCount((int) duplicateCount);
        result.setFailRatePct(dupRate);
        result.setPassed(dupRate <= maxDupRate);
        result.setMessage(String.format("%s: %.1f%% duplicate values (threshold: %.1f%%)", column, dupRate, maxDupRate));
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private Map<String, Object> parseParams(String paramsJson) {
        if (paramsJson == null || paramsJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(paramsJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse rule params JSON: {}", e.getMessage());
            return Map.of();
        }
    }

    private double getDouble(Map<String, Object> params, String key, double defaultValue) {
        Object val = params.get(key);
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (NumberFormatException e) { return defaultValue; }
    }

    private int getInt(Map<String, Object> params, String key, int defaultValue) {
        Object val = params.get(key);
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).intValue();
        try { return Integer.parseInt(val.toString()); } catch (NumberFormatException e) { return defaultValue; }
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}

