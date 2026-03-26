package com.datastd.quality.engine;

import com.datastd.common.dto.OverallStatus;
import com.datastd.common.dto.QualityReport;
import com.datastd.common.dto.ValidationRuleResult;
import com.datastd.quality.engine.validation.*;
import com.datastd.quality.entity.Severity;
import com.datastd.quality.entity.ValidationRule;
import com.datastd.quality.entity.ValidationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for the {@link ValidationEngine}.
 * Covers all acceptance criteria from the feature spec.
 */
class ValidationEngineTest {

    private ValidationEngine engine;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ValidationStrategyFactory strategyFactory = new ValidationStrategyFactory(
                new NotNullValidation(), new NotEmptyValidation(),
                new RegexMatchValidation(), new AllowedValuesValidation(),
                new NumericRangeValidation(), new MinLengthValidation(),
                new MaxLengthValidation(), new UniqueValidation(),
                new CustomSqlValidation()
        );
        engine = new ValidationEngine(objectMapper, new ColumnProfiler(), strategyFactory);
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private ValidationRule rule(String name, String column, ValidationType type,
                                 Severity severity, String params) {
        ValidationRule r = new ValidationRule();
        r.setId(UUID.randomUUID());
        r.setName(name);
        r.setColumnName(column);
        r.setValidationType(type);
        r.setSeverity(severity);
        r.setParams(params);
        r.setActive(true);
        return r;
    }

    private Map<String, Object> row(Object... kvPairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < kvPairs.length; i += 2) {
            map.put((String) kvPairs[i], kvPairs[i + 1]);
        }
        return map;
    }

    // ── NOT_NULL ────────────────────────────────────────────────────

    @Nested
    @DisplayName("NOT_NULL validation")
    class NotNullTests {

        @Test
        @DisplayName("correctly computes nullRatePct — AC: nullRatePct for NOT_NULL rule")
        void testNullRateCalculation() {
            List<Map<String, Object>> records = List.of(
                    row("email", "a@b.com"),
                    row("email", null),
                    row("email", "c@d.com"),
                    row("email", null),
                    row("email", "e@f.com")
            );

            ValidationRule notNull = rule("email-not-null", "email", ValidationType.NOT_NULL,
                    Severity.ERROR, "{\"maxNullRatePct\": 10.0}");

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of(notNull));

            assertEquals(1, report.getRuleResults().size());
            ValidationRuleResult result = report.getRuleResults().get(0);
            assertEquals(40.0, result.getFailRatePct());
            assertEquals(2, result.getFailCount());
            assertFalse(result.isPassed());
        }

        @Test
        @DisplayName("passes when null rate is within threshold")
        void testNullRateBelowThreshold() {
            List<Map<String, Object>> records = List.of(
                    row("name", "Alice"),
                    row("name", "Bob"),
                    row("name", null),
                    row("name", "Charlie"),
                    row("name", "Diana")
            );

            ValidationRule notNull = rule("name-not-null", "name", ValidationType.NOT_NULL,
                    Severity.WARNING, "{\"maxNullRatePct\": 25.0}");

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of(notNull));
            assertTrue(report.getRuleResults().get(0).isPassed());
            assertEquals(OverallStatus.PASS, report.getOverallStatus());
        }
    }

    // ── NOT_EMPTY ───────────────────────────────────────────────────

    @Nested
    @DisplayName("NOT_EMPTY validation")
    class NotEmptyTests {

        @Test
        @DisplayName("detects blank strings")
        void testBlankDetection() {
            List<Map<String, Object>> records = List.of(
                    row("city", "NYC"),
                    row("city", ""),
                    row("city", "  "),
                    row("city", null),
                    row("city", "LA")
            );

            ValidationRule notEmpty = rule("city-not-empty", "city", ValidationType.NOT_EMPTY,
                    Severity.ERROR, "{\"maxNullRatePct\": 0.0}");

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of(notEmpty));
            ValidationRuleResult result = report.getRuleResults().get(0);
            assertEquals(3, result.getFailCount());
            assertEquals(60.0, result.getFailRatePct());
            assertFalse(result.isPassed());
        }
    }

    // ── REGEX_MATCH ─────────────────────────────────────────────────

    @Nested
    @DisplayName("REGEX_MATCH validation")
    class RegexMatchTests {

        @Test
        @DisplayName("validates email pattern")
        void testEmailRegex() {
            List<Map<String, Object>> records = List.of(
                    row("email", "alice@example.com"),
                    row("email", "not-an-email"),
                    row("email", "bob@test.org"),
                    row("email", "bad@"),
                    row("email", "valid@domain.co")
            );

            ValidationRule regex = rule("email-regex", "email", ValidationType.REGEX_MATCH,
                    Severity.ERROR,
                    "{\"pattern\": \"^[\\\\w.+]+@[\\\\w.]+\\\\.[a-z]{2,}$\", \"maxFailRatePct\": 0.0}");

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of(regex));
            ValidationRuleResult result = report.getRuleResults().get(0);
            assertFalse(result.isPassed());
            assertEquals(2, result.getFailCount());
        }
    }

    // ── ALLOWED_VALUES ──────────────────────────────────────────────

    @Nested
    @DisplayName("ALLOWED_VALUES validation")
    class AllowedValuesTests {

        @Test
        @DisplayName("rejects values not in allowed set")
        void testAllowedValues() {
            List<Map<String, Object>> records = List.of(
                    row("gender", "Male"),
                    row("gender", "Female"),
                    row("gender", "Other"),
                    row("gender", "Unknown"),
                    row("gender", "Male")
            );

            ValidationRule allowed = rule("gender-allowed", "gender", ValidationType.ALLOWED_VALUES,
                    Severity.WARNING,
                    "{\"values\": [\"Male\", \"Female\", \"Other\"], \"maxFailRatePct\": 0.0}");

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of(allowed));
            ValidationRuleResult result = report.getRuleResults().get(0);
            assertFalse(result.isPassed());
            assertEquals(1, result.getFailCount());
        }
    }

    // ── NUMERIC_RANGE ───────────────────────────────────────────────

    @Nested
    @DisplayName("NUMERIC_RANGE validation")
    class NumericRangeTests {

        @Test
        @DisplayName("detects values outside range")
        void testNumericRange() {
            List<Map<String, Object>> records = List.of(
                    row("age", 25),
                    row("age", 150),
                    row("age", -5),
                    row("age", 0),
                    row("age", 200)
            );

            ValidationRule range = rule("age-range", "age", ValidationType.NUMERIC_RANGE,
                    Severity.ERROR,
                    "{\"min\": 0, \"max\": 150, \"maxFailRatePct\": 0.0}");

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of(range));
            ValidationRuleResult result = report.getRuleResults().get(0);
            assertFalse(result.isPassed());
            assertEquals(2, result.getFailCount()); // -5 and 200
        }
    }

    // ── MIN_LENGTH ──────────────────────────────────────────────────

    @Nested
    @DisplayName("MIN_LENGTH validation")
    class MinLengthTests {

        @Test
        @DisplayName("detects strings shorter than minimum")
        void testMinLength() {
            List<Map<String, Object>> records = List.of(
                    row("code", "AB"),
                    row("code", "A"),
                    row("code", "ABC"),
                    row("code", ""),
                    row("code", "XY")
            );

            ValidationRule minLen = rule("code-min-len", "code", ValidationType.MIN_LENGTH,
                    Severity.WARNING, "{\"length\": 2}");

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of(minLen));
            ValidationRuleResult result = report.getRuleResults().get(0);
            assertFalse(result.isPassed());
            assertEquals(2, result.getFailCount()); // "A" and ""
        }
    }

    // ── MAX_LENGTH ──────────────────────────────────────────────────

    @Nested
    @DisplayName("MAX_LENGTH validation")
    class MaxLengthTests {

        @Test
        @DisplayName("detects strings longer than maximum")
        void testMaxLength() {
            List<Map<String, Object>> records = List.of(
                    row("zip", "12345"),
                    row("zip", "123456"),
                    row("zip", "1234"),
                    row("zip", "12345678")
            );

            ValidationRule maxLen = rule("zip-max-len", "zip", ValidationType.MAX_LENGTH,
                    Severity.ERROR, "{\"length\": 5}");

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of(maxLen));
            ValidationRuleResult result = report.getRuleResults().get(0);
            assertFalse(result.isPassed());
            assertEquals(2, result.getFailCount()); // "123456" and "12345678"
        }
    }

    // ── UNIQUE ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("UNIQUE validation")
    class UniqueTests {

        @Test
        @DisplayName("detects duplicate values in column")
        void testUnique() {
            List<Map<String, Object>> records = List.of(
                    row("id", "A1"),
                    row("id", "A2"),
                    row("id", "A1"),
                    row("id", "A3"),
                    row("id", "A2")
            );

            ValidationRule unique = rule("id-unique", "id", ValidationType.UNIQUE,
                    Severity.ERROR, "{\"maxDuplicateRatePct\": 0.0}");

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of(unique));
            ValidationRuleResult result = report.getRuleResults().get(0);
            assertFalse(result.isPassed());
            assertEquals(2, result.getFailCount()); // A1 dup + A2 dup
        }
    }

    // ── Duplicate Detection (row-level) ─────────────────────────────

    @Nested
    @DisplayName("Row-level duplicate detection")
    class DuplicateDetectionTests {

        @Test
        @DisplayName("correctly detects duplicates using row hashing — AC: row hashing")
        void testRowDuplicates() {
            List<Map<String, Object>> records = List.of(
                    row("name", "Alice", "age", 30),
                    row("name", "Bob", "age", 25),
                    row("name", "Alice", "age", 30),  // duplicate of row 0
                    row("name", "Charlie", "age", 35),
                    row("name", "Bob", "age", 25)       // duplicate of row 1
            );

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of());
            assertEquals(2, report.getDuplicateCount());
        }

        @Test
        @DisplayName("no duplicates when all rows unique")
        void testNoDuplicates() {
            List<Map<String, Object>> records = List.of(
                    row("name", "Alice", "age", 30),
                    row("name", "Bob", "age", 25),
                    row("name", "Charlie", "age", 35)
            );

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of());
            assertEquals(0, report.getDuplicateCount());
        }
    }

    // ── Quality Score & Overall Status ──────────────────────────────

    @Nested
    @DisplayName("Quality score and overall status computation")
    class QualityScoreTests {

        @Test
        @DisplayName("qualityScore decreases by 20 per ERROR violation — AC: 20 per ERROR, 5 per WARNING")
        void testErrorPenalty() {
            List<Map<String, Object>> records = List.of(
                    row("email", null),
                    row("email", "valid@test.com")
            );

            ValidationRule errorRule = rule("email-not-null", "email", ValidationType.NOT_NULL,
                    Severity.ERROR, "{\"maxNullRatePct\": 0.0}");

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of(errorRule));
            assertEquals(80, report.getQualityScore()); // 100 - 20
            assertEquals(OverallStatus.FAIL, report.getOverallStatus());
        }

        @Test
        @DisplayName("qualityScore decreases by 5 per WARNING violation")
        void testWarningPenalty() {
            List<Map<String, Object>> records = List.of(
                    row("name", null),
                    row("name", "Alice")
            );

            ValidationRule warnRule = rule("name-not-null", "name", ValidationType.NOT_NULL,
                    Severity.WARNING, "{\"maxNullRatePct\": 0.0}");

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of(warnRule));
            assertEquals(95, report.getQualityScore()); // 100 - 5
            assertEquals(OverallStatus.WARN, report.getOverallStatus());
        }

        @Test
        @DisplayName("qualityScore = 60 when 2 ERROR rules fail — AC: score 60 with 2 errors")
        void testTwoErrorsFail() {
            List<Map<String, Object>> records = List.of(
                    row("email", null, "name", null),
                    row("email", "valid@test.com", "name", "Alice")
            );

            ValidationRule rule1 = rule("email-not-null", "email", ValidationType.NOT_NULL,
                    Severity.ERROR, "{\"maxNullRatePct\": 0.0}");
            ValidationRule rule2 = rule("name-not-null", "name", ValidationType.NOT_NULL,
                    Severity.ERROR, "{\"maxNullRatePct\": 0.0}");

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of(rule1, rule2));
            assertEquals(60, report.getQualityScore()); // 100 - 20 - 20
            assertEquals(OverallStatus.FAIL, report.getOverallStatus());
        }

        @Test
        @DisplayName("overallStatus = FAIL when any ERROR rule fails — AC: FAIL on ERROR")
        void testFailOnError() {
            List<Map<String, Object>> records = List.of(
                    row("value", null)
            );

            ValidationRule errorRule = rule("val-not-null", "value", ValidationType.NOT_NULL,
                    Severity.ERROR, "{\"maxNullRatePct\": 0.0}");

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of(errorRule));
            assertEquals(OverallStatus.FAIL, report.getOverallStatus());
        }

        @Test
        @DisplayName("overallStatus = WARN when only WARNING rules fail — AC: WARN on WARNING only")
        void testWarnOnWarning() {
            List<Map<String, Object>> records = List.of(
                    row("value", null)
            );

            ValidationRule warnRule = rule("val-not-null", "value", ValidationType.NOT_NULL,
                    Severity.WARNING, "{\"maxNullRatePct\": 0.0}");

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of(warnRule));
            assertEquals(OverallStatus.WARN, report.getOverallStatus());
        }

        @Test
        @DisplayName("overallStatus = PASS when all rules pass")
        void testPassWhenAllGood() {
            List<Map<String, Object>> records = List.of(
                    row("email", "alice@example.com"),
                    row("email", "bob@example.com")
            );

            ValidationRule notNull = rule("email-not-null", "email", ValidationType.NOT_NULL,
                    Severity.ERROR, "{\"maxNullRatePct\": 0.0}");

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of(notNull));
            assertEquals(100, report.getQualityScore());
            assertEquals(OverallStatus.PASS, report.getOverallStatus());
        }

        @Test
        @DisplayName("qualityScore capped at 0 when many violations")
        void testScoreCappedAtZero() {
            List<Map<String, Object>> records = List.of(
                    row("a", null, "b", null, "c", null, "d", null, "e", null, "f", null)
            );

            List<ValidationRule> rules = new ArrayList<>();
            for (String col : List.of("a", "b", "c", "d", "e", "f")) {
                rules.add(rule(col + "-not-null", col, ValidationType.NOT_NULL,
                        Severity.ERROR, "{\"maxNullRatePct\": 0.0}"));
            }

            QualityReport report = engine.validate(UUID.randomUUID(), records, rules);
            assertEquals(0, report.getQualityScore()); // 100 - (6 * 20) = -20, capped to 0
        }

        @Test
        @DisplayName("mixed ERROR and WARNING penalties")
        void testMixedPenalties() {
            List<Map<String, Object>> records = List.of(
                    row("email", null, "name", null)
            );

            ValidationRule errorRule = rule("email-not-null", "email", ValidationType.NOT_NULL,
                    Severity.ERROR, "{\"maxNullRatePct\": 0.0}");
            ValidationRule warnRule = rule("name-not-null", "name", ValidationType.NOT_NULL,
                    Severity.WARNING, "{\"maxNullRatePct\": 0.0}");

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of(errorRule, warnRule));
            assertEquals(75, report.getQualityScore()); // 100 - 20 - 5
            assertEquals(OverallStatus.FAIL, report.getOverallStatus()); // ERROR present → FAIL
        }
    }

    // ── Column Reports ──────────────────────────────────────────────

    @Nested
    @DisplayName("Column-level statistics")
    class ColumnReportTests {

        @Test
        @DisplayName("computes column nullRate and uniqueRate")
        void testColumnStats() {
            List<Map<String, Object>> records = List.of(
                    row("city", "NYC"),
                    row("city", "NYC"),
                    row("city", null),
                    row("city", "LA"),
                    row("city", "LA")
            );

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of());
            assertEquals(1, report.getColumnReports().size());

            var colReport = report.getColumnReports().get(0);
            assertEquals("city", colReport.getColumnName());
            assertEquals(20.0, colReport.getNullRatePct()); // 1/5
            assertEquals(40.0, colReport.getUniqueRatePct()); // 2 distinct / 5 total
        }

        @Test
        @DisplayName("provides sample values (max 5)")
        void testSampleValues() {
            List<Map<String, Object>> records = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                records.add(row("code", "V" + i));
            }

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of());
            var colReport = report.getColumnReports().get(0);
            assertEquals(5, colReport.getSampleValues().size());
        }
    }

    // ── Wildcard Column ─────────────────────────────────────────────

    @Nested
    @DisplayName("Wildcard column (*)")
    class WildcardColumnTests {

        @Test
        @DisplayName("applies rule to all columns when column = *")
        void testWildcard() {
            List<Map<String, Object>> records = List.of(
                    row("a", null, "b", null),
                    row("a", "val", "b", "val")
            );

            ValidationRule wildcardRule = rule("all-not-null", "*", ValidationType.NOT_NULL,
                    Severity.WARNING, "{\"maxNullRatePct\": 0.0}");

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of(wildcardRule));
            assertEquals(2, report.getRuleResults().size()); // one for "a", one for "b"
        }
    }

    // ── Inactive Rules ──────────────────────────────────────────────

    @Nested
    @DisplayName("Inactive rules")
    class InactiveRuleTests {

        @Test
        @DisplayName("skips inactive rules")
        void testInactiveSkipped() {
            List<Map<String, Object>> records = List.of(row("x", null));

            ValidationRule inactive = rule("x-not-null", "x", ValidationType.NOT_NULL,
                    Severity.ERROR, "{\"maxNullRatePct\": 0.0}");
            inactive.setActive(false);

            QualityReport report = engine.validate(UUID.randomUUID(), records, List.of(inactive));
            assertTrue(report.getRuleResults().isEmpty());
            assertEquals(100, report.getQualityScore());
            assertEquals(OverallStatus.PASS, report.getOverallStatus());
        }
    }

    // ── Empty records ───────────────────────────────────────────────

    @Nested
    @DisplayName("Empty records")
    class EmptyRecordTests {

        @Test
        @DisplayName("handles empty record list")
        void testEmptyRecords() {
            ValidationRule rule = rule("x-not-null", "x", ValidationType.NOT_NULL,
                    Severity.ERROR, "{\"maxNullRatePct\": 0.0}");

            QualityReport report = engine.validate(UUID.randomUUID(), List.of(), List.of(rule));
            assertEquals(0, report.getTotalRecords());
            assertEquals(0, report.getDuplicateCount());
            assertEquals(OverallStatus.PASS, report.getOverallStatus());
        }
    }
}

