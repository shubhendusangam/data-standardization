package com.datastd.quality.engine;

import com.datastd.common.dto.ColumnReport;
import com.datastd.common.dto.InferredType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ColumnProfiler — type inference, profiling stats, and edge cases.
 */
class ColumnProfilerTest {

    private ColumnProfiler profiler;

    @BeforeEach
    void setUp() {
        profiler = new ColumnProfiler();
    }

    // ── Type Inference ──────────────────────────────────────────────

    @Test
    void inferType_allIntegers() {
        List<Map<String, Object>> records = List.of(
                Map.of("age", "25"), Map.of("age", "30"), Map.of("age", "45"),
                Map.of("age", "18"), Map.of("age", "60"));
        ColumnReport report = profiler.profileColumn("age", records);
        assertThat(report.getInferredType()).isEqualTo(InferredType.INTEGER);
    }

    @Test
    void inferType_allDecimals() {
        List<Map<String, Object>> records = List.of(
                Map.of("price", "19.99"), Map.of("price", "5.50"), Map.of("price", "100.0"),
                Map.of("price", "0.99"), Map.of("price", "49.95"));
        ColumnReport report = profiler.profileColumn("price", records);
        assertThat(report.getInferredType()).isEqualTo(InferredType.DECIMAL);
    }

    @Test
    void inferType_allDates_isoFormat() {
        List<Map<String, Object>> records = List.of(
                Map.of("dob", "1990-01-15"), Map.of("dob", "1985-06-20"),
                Map.of("dob", "2000-12-31"), Map.of("dob", "1975-03-10"),
                Map.of("dob", "1995-07-04"));
        ColumnReport report = profiler.profileColumn("dob", records);
        assertThat(report.getInferredType()).isEqualTo(InferredType.DATE);
    }

    @Test
    void inferType_allDates_usFormat() {
        List<Map<String, Object>> records = List.of(
                Map.of("dob", "03/15/1990"), Map.of("dob", "06/20/1985"),
                Map.of("dob", "12/31/2000"), Map.of("dob", "03/10/1975"),
                Map.of("dob", "07/04/1995"));
        ColumnReport report = profiler.profileColumn("dob", records);
        assertThat(report.getInferredType()).isEqualTo(InferredType.DATE);
    }

    @Test
    void inferType_allBooleans_trueFalse() {
        List<Map<String, Object>> records = List.of(
                Map.of("active", "true"), Map.of("active", "false"),
                Map.of("active", "true"), Map.of("active", "false"),
                Map.of("active", "true"));
        ColumnReport report = profiler.profileColumn("active", records);
        assertThat(report.getInferredType()).isEqualTo(InferredType.BOOLEAN);
    }

    @Test
    void inferType_allBooleans_yesNo() {
        List<Map<String, Object>> records = List.of(
                Map.of("active", "yes"), Map.of("active", "no"),
                Map.of("active", "YES"), Map.of("active", "NO"),
                Map.of("active", "y"));
        ColumnReport report = profiler.profileColumn("active", records);
        assertThat(report.getInferredType()).isEqualTo(InferredType.BOOLEAN);
    }

    @Test
    void inferType_allStrings() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "John"), Map.of("name", "Jane"),
                Map.of("name", "Alice"), Map.of("name", "Bob"));
        ColumnReport report = profiler.profileColumn("name", records);
        assertThat(report.getInferredType()).isEqualTo(InferredType.STRING);
    }

    @Test
    void inferType_mixed_belowThreshold() {
        // 3 strings, 2 integers, 2 dates, 2 booleans, 1 decimal = 10 total
        // Highest is string at 30% < 70% → MIXED
        List<Map<String, Object>> records = List.of(
                Map.of("col", "hello"), Map.of("col", "world"), Map.of("col", "foo"),
                Map.of("col", "42"), Map.of("col", "99"),
                Map.of("col", "2024-01-01"), Map.of("col", "2024-06-15"),
                Map.of("col", "true"), Map.of("col", "false"),
                Map.of("col", "3.14"));
        ColumnReport report = profiler.profileColumn("col", records);
        assertThat(report.getInferredType()).isEqualTo(InferredType.MIXED);
    }

    @Test
    void inferType_emptyColumn_returnsUnknown() {
        List<Map<String, Object>> records = List.of(
                new HashMap<>(Map.of("other", "val")),
                new HashMap<>(Map.of("other", "val")));
        // "col" is missing from all records → all nulls
        ColumnReport report = profiler.profileColumn("col", records);
        assertThat(report.getInferredType()).isEqualTo(InferredType.UNKNOWN);
    }

    @Test
    void inferType_javaBooleanObjects() {
        List<Map<String, Object>> records = List.of(
                Map.of("flag", true), Map.of("flag", false),
                Map.of("flag", true), Map.of("flag", true));
        ColumnReport report = profiler.profileColumn("flag", records);
        assertThat(report.getInferredType()).isEqualTo(InferredType.BOOLEAN);
    }

    @Test
    void inferType_javaIntegerObjects() {
        List<Map<String, Object>> records = List.of(
                Map.of("count", 1), Map.of("count", 2),
                Map.of("count", 3), Map.of("count", 4));
        ColumnReport report = profiler.profileColumn("count", records);
        assertThat(report.getInferredType()).isEqualTo(InferredType.INTEGER);
    }

    @Test
    void inferType_javaDoubleObjects() {
        List<Map<String, Object>> records = List.of(
                Map.of("score", 1.5), Map.of("score", 2.7),
                Map.of("score", 3.3), Map.of("score", 4.9));
        ColumnReport report = profiler.profileColumn("score", records);
        assertThat(report.getInferredType()).isEqualTo(InferredType.DECIMAL);
    }

    // ── Profiling Stats ─────────────────────────────────────────────

    @Test
    void profileColumn_computesNullStats() {
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> r = new HashMap<>();
            r.put("email", i < 3 ? null : "user" + i + "@test.com");
            records.add(r);
        }

        ColumnReport report = profiler.profileColumn("email", records);
        assertThat(report.getTotalCount()).isEqualTo(10);
        assertThat(report.getNullCount()).isEqualTo(3);
        assertThat(report.getNullRatePct()).isEqualTo(30.0);
    }

    @Test
    void profileColumn_computesUniqueStats() {
        List<Map<String, Object>> records = List.of(
                Map.of("color", "red"), Map.of("color", "blue"),
                Map.of("color", "red"), Map.of("color", "green"),
                Map.of("color", "blue"));

        ColumnReport report = profiler.profileColumn("color", records);
        assertThat(report.getUniqueCount()).isEqualTo(3);
        assertThat(report.getUniqueRatePct()).isEqualTo(60.0);
    }

    @Test
    void profileColumn_computesStringLengths() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Jo"), Map.of("name", "Alice"),
                Map.of("name", "Bob"));

        ColumnReport report = profiler.profileColumn("name", records);
        assertThat(report.getMinLength()).isEqualTo(2);
        assertThat(report.getMaxLength()).isEqualTo(5);
        assertThat(report.getAvgLength()).isCloseTo(3.3, org.assertj.core.data.Offset.offset(0.1));
    }

    @Test
    void profileColumn_computesNumericStats() {
        List<Map<String, Object>> records = List.of(
                Map.of("age", "20"), Map.of("age", "30"),
                Map.of("age", "40"), Map.of("age", "50"));

        ColumnReport report = profiler.profileColumn("age", records);
        assertThat(report.getMin()).isEqualTo(20.0);
        assertThat(report.getMax()).isEqualTo(50.0);
        assertThat(report.getMean()).isEqualTo(35.0);
        assertThat(report.getStddev()).isNotNull();
    }

    @Test
    void profileColumn_topValues_limitedTo10() {
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            records.add(Map.of("val", "item" + i));
        }

        ColumnReport report = profiler.profileColumn("val", records);
        assertThat(report.getTopValues()).hasSize(10);
    }

    @Test
    void profileColumn_topValues_sortedByFrequency() {
        List<Map<String, Object>> records = List.of(
                Map.of("color", "red"), Map.of("color", "red"), Map.of("color", "red"),
                Map.of("color", "blue"), Map.of("color", "blue"),
                Map.of("color", "green"));

        ColumnReport report = profiler.profileColumn("color", records);
        assertThat(report.getTopValues()).isNotEmpty();
        assertThat(report.getTopValues().get(0).getValue()).isEqualTo("red");
        assertThat(report.getTopValues().get(0).getCount()).isEqualTo(3);
    }

    @Test
    void profileColumn_sampleValues_limitedTo5() {
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            records.add(Map.of("val", "distinct" + i));
        }

        ColumnReport report = profiler.profileColumn("val", records);
        assertThat(report.getSampleValues()).hasSize(5);
    }

    // ── Full Dataset Profiling ──────────────────────────────────────

    @Test
    void profileDataset_profilesAllColumns() {
        List<Map<String, Object>> records = List.of(
                Map.of("name", "John", "age", "25", "email", "john@test.com"),
                Map.of("name", "Jane", "age", "30", "email", "jane@test.com"));

        List<ColumnReport> profiles = profiler.profileDataset(records);
        assertThat(profiles).hasSize(3);
        assertThat(profiles.stream().map(ColumnReport::getColumnName).toList())
                .containsExactlyInAnyOrder("name", "age", "email");
    }

    // ── Date Pattern Detection ──────────────────────────────────────

    @Test
    void detectDatePattern_iso() {
        String pattern = profiler.detectDatePattern(List.of("2024-01-15", "2024-06-20", "2024-12-31"));
        assertThat(pattern).isEqualTo("yyyy-MM-dd");
    }

    @Test
    void detectDatePattern_usFormat() {
        String pattern = profiler.detectDatePattern(List.of("01/15/2024", "06/20/2024", "12/31/2024"));
        assertThat(pattern).isEqualTo("MM/dd/yyyy");
    }

    // ── Static Helpers ──────────────────────────────────────────────

    @Test
    void isInteger_valid() {
        assertThat(ColumnProfiler.isInteger("42")).isTrue();
        assertThat(ColumnProfiler.isInteger("-100")).isTrue();
        assertThat(ColumnProfiler.isInteger("3.14")).isFalse();
        assertThat(ColumnProfiler.isInteger("abc")).isFalse();
    }

    @Test
    void isDecimal_valid() {
        assertThat(ColumnProfiler.isDecimal("3.14")).isTrue();
        assertThat(ColumnProfiler.isDecimal("-0.5")).isTrue();
        assertThat(ColumnProfiler.isDecimal("42")).isFalse(); // integer, not decimal
        assertThat(ColumnProfiler.isDecimal("abc")).isFalse();
    }

    @Test
    void isBoolean_valid() {
        assertThat(ColumnProfiler.isBoolean("true")).isTrue();
        assertThat(ColumnProfiler.isBoolean("FALSE")).isTrue();
        assertThat(ColumnProfiler.isBoolean("yes")).isTrue();
        assertThat(ColumnProfiler.isBoolean("No")).isTrue();
        assertThat(ColumnProfiler.isBoolean("1")).isTrue();
        assertThat(ColumnProfiler.isBoolean("0")).isTrue();
        assertThat(ColumnProfiler.isBoolean("y")).isTrue();
        assertThat(ColumnProfiler.isBoolean("n")).isTrue();
        assertThat(ColumnProfiler.isBoolean("maybe")).isFalse();
    }

    @Test
    void isDate_valid() {
        assertThat(ColumnProfiler.isDate("2024-01-15")).isTrue();
        assertThat(ColumnProfiler.isDate("03/15/1990")).isTrue();
        assertThat(ColumnProfiler.isDate("15-03-1990")).isTrue();
        assertThat(ColumnProfiler.isDate("not a date")).isFalse();
        assertThat(ColumnProfiler.isDate("42")).isFalse();
    }

    // ── Performance ─────────────────────────────────────────────────

    @Test
    void profiler_handles10000Rows_under2Seconds() {
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 0; i < 10_000; i++) {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("id", String.valueOf(i));
            record.put("name", "User" + i);
            record.put("email", "user" + i + "@test.com");
            record.put("age", String.valueOf(20 + (i % 60)));
            record.put("active", i % 2 == 0 ? "true" : "false");
            records.add(record);
        }

        long start = System.currentTimeMillis();
        List<ColumnReport> profiles = profiler.profileDataset(records);
        long elapsed = System.currentTimeMillis() - start;

        assertThat(profiles).hasSize(5);
        assertThat(elapsed).isLessThan(2000);
    }
}

