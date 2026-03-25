package com.datastd.common.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Shared DTO representing the result of a data-quality validation run.
 * Returned by data-quality-service and consumed by standardization-service
 * via its Feign client.
 */
public class QualityReport {

    private UUID reportId;
    private UUID datasetId;
    private OverallStatus overallStatus;
    private int qualityScore;
    private int totalRecords;
    private int duplicateCount;
    private List<ColumnReport> columnReports;
    private List<ValidationRuleResult> ruleResults;
    private Instant evaluatedAt;

    public QualityReport() {}

    // Getters and Setters

    public UUID getReportId() {
        return reportId;
    }

    public void setReportId(UUID reportId) {
        this.reportId = reportId;
    }

    public UUID getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(UUID datasetId) {
        this.datasetId = datasetId;
    }

    public OverallStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(OverallStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    public int getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(int qualityScore) {
        this.qualityScore = qualityScore;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(int duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public List<ColumnReport> getColumnReports() {
        return columnReports;
    }

    public void setColumnReports(List<ColumnReport> columnReports) {
        this.columnReports = columnReports;
    }

    public List<ValidationRuleResult> getRuleResults() {
        return ruleResults;
    }

    public void setRuleResults(List<ValidationRuleResult> ruleResults) {
        this.ruleResults = ruleResults;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}

