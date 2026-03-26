package com.datastd.quality.engine;

import com.datastd.common.dto.*;
import com.datastd.quality.engine.validation.ValidationStrategy;
import com.datastd.quality.engine.validation.ValidationStrategyFactory;
import com.datastd.quality.entity.ValidationRule;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

/**
 * Core validation engine that evaluates a list of records against validation rules
 * and produces a {@link QualityReport}.
 * <p>
 * Uses the Strategy pattern: each {@link com.datastd.quality.entity.ValidationType}
 * is handled by a dedicated {@link ValidationStrategy} implementation,
 * resolved via {@link ValidationStrategyFactory}.
 */
@Component
public class ValidationEngine {

    private static final Logger log = LoggerFactory.getLogger(ValidationEngine.class);
    private static final int ERROR_PENALTY = 20;
    private static final int WARNING_PENALTY = 5;

    private final ObjectMapper objectMapper;
    private final ColumnProfiler columnProfiler;
    private final ValidationStrategyFactory validationStrategyFactory;

    public ValidationEngine(ObjectMapper objectMapper, ColumnProfiler columnProfiler,
                            ValidationStrategyFactory validationStrategyFactory) {
        this.objectMapper = objectMapper;
        this.columnProfiler = columnProfiler;
        this.validationStrategyFactory = validationStrategyFactory;
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
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ── Per-Rule Evaluation (delegates to Strategy) ─────────────────

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

        ValidationStrategy strategy = validationStrategyFactory.getStrategy(rule.getValidationType());
        strategy.evaluate(result, columnName, records, params);

        return result;
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
}

