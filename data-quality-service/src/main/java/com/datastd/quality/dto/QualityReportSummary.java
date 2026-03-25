package com.datastd.quality.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight summary of a QualityReport — excludes columnReports and ruleResults.
 */
public class QualityReportSummary {

    private UUID reportId;
    private UUID datasetId;
    private String overallStatus;
    private int qualityScore;
    private Instant evaluatedAt;
    private int totalRecords;
    private int duplicateCount;

    public QualityReportSummary() {}

    public QualityReportSummary(UUID reportId, UUID datasetId, String overallStatus,
                                int qualityScore, Instant evaluatedAt, int totalRecords, int duplicateCount) {
        this.reportId = reportId;
        this.datasetId = datasetId;
        this.overallStatus = overallStatus;
        this.qualityScore = qualityScore;
        this.evaluatedAt = evaluatedAt;
        this.totalRecords = totalRecords;
        this.duplicateCount = duplicateCount;
    }

    public UUID getReportId() { return reportId; }
    public void setReportId(UUID reportId) { this.reportId = reportId; }
    public UUID getDatasetId() { return datasetId; }
    public void setDatasetId(UUID datasetId) { this.datasetId = datasetId; }
    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }
    public int getQualityScore() { return qualityScore; }
    public void setQualityScore(int qualityScore) { this.qualityScore = qualityScore; }
    public Instant getEvaluatedAt() { return evaluatedAt; }
    public void setEvaluatedAt(Instant evaluatedAt) { this.evaluatedAt = evaluatedAt; }
    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
    public int getDuplicateCount() { return duplicateCount; }
    public void setDuplicateCount(int duplicateCount) { this.duplicateCount = duplicateCount; }
}

