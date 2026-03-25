package com.datastd.quality.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Persisted quality report. The full JSON report is stored as a LOB.
 * One report per (datasetId, evaluatedAt) pair.
 */
@Entity
@Table(name = "quality_reports")
public class QualityReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID datasetId;

    @Column(nullable = false)
    private String overallStatus;

    @Column(nullable = false)
    private int qualityScore;

    @Column(nullable = false)
    private int totalRecords;

    @Column(nullable = false)
    private int duplicateCount;

    @Lob
    @Column(columnDefinition = "CLOB", nullable = false)
    private String reportJson;

    @Column(nullable = false)
    private Instant evaluatedAt;

    @PrePersist
    protected void onCreate() {
        if (evaluatedAt == null) {
            evaluatedAt = Instant.now();
        }
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(UUID datasetId) {
        this.datasetId = datasetId;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
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

    public String getReportJson() {
        return reportJson;
    }

    public void setReportJson(String reportJson) {
        this.reportJson = reportJson;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(Instant evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }
}

