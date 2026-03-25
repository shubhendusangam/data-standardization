package com.datastd.common.dto;

import java.util.List;

/**
 * Per-column statistics computed during quality validation.
 * Extended with full profiling stats: counts, string lengths,
 * numeric stats, top values, and enhanced type inference.
 */
public class ColumnReport {

    private String columnName;
    private double nullRatePct;
    private double uniqueRatePct;
    private List<String> sampleValues;
    private InferredType inferredType;

    // ── Extended profiling fields ────────────────────────────────────
    private int totalCount;
    private int nullCount;
    private int uniqueCount;
    private Integer minLength;
    private Integer maxLength;
    private Double avgLength;
    private Double min;
    private Double max;
    private Double mean;
    private Double stddev;
    private List<TopValue> topValues;

    public ColumnReport() {}

    // ── Top frequent value DTO ───────────────────────────────────────

    public static class TopValue {
        private String value;
        private int count;
        private double pct;

        public TopValue() {}

        public TopValue(String value, int count, double pct) {
            this.value = value;
            this.count = count;
            this.pct = pct;
        }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        public double getPct() { return pct; }
        public void setPct(double pct) { this.pct = pct; }
    }

    // Getters and Setters

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public double getNullRatePct() {
        return nullRatePct;
    }

    public void setNullRatePct(double nullRatePct) {
        this.nullRatePct = nullRatePct;
    }

    public double getUniqueRatePct() {
        return uniqueRatePct;
    }

    public void setUniqueRatePct(double uniqueRatePct) {
        this.uniqueRatePct = uniqueRatePct;
    }

    public List<String> getSampleValues() {
        return sampleValues;
    }

    public void setSampleValues(List<String> sampleValues) {
        this.sampleValues = sampleValues;
    }

    public InferredType getInferredType() {
        return inferredType;
    }

    public void setInferredType(InferredType inferredType) {
        this.inferredType = inferredType;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getNullCount() {
        return nullCount;
    }

    public void setNullCount(int nullCount) {
        this.nullCount = nullCount;
    }

    public int getUniqueCount() {
        return uniqueCount;
    }

    public void setUniqueCount(int uniqueCount) {
        this.uniqueCount = uniqueCount;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public Double getAvgLength() {
        return avgLength;
    }

    public void setAvgLength(Double avgLength) {
        this.avgLength = avgLength;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public Double getMean() {
        return mean;
    }

    public void setMean(Double mean) {
        this.mean = mean;
    }

    public Double getStddev() {
        return stddev;
    }

    public void setStddev(Double stddev) {
        this.stddev = stddev;
    }

    public List<TopValue> getTopValues() {
        return topValues;
    }

    public void setTopValues(List<TopValue> topValues) {
        this.topValues = topValues;
    }
}
