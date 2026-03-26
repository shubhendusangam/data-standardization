package com.datastd.standardization.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "processing_jobs")
public class ProcessingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID datasetId;

    private UUID ruleSetId;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String ruleIds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    private int totalRecords;
    private int processedRecords;
    private int errorCount;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String resultData;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String errorLog;

    @Lob
    @Column(columnDefinition = "CLOB")
    private String ruleApplicationErrorsJson;

    private UUID qualityReportId;

    private Integer qualityScore;

    private String qualityStatus;

    private boolean skipQualityCheck;

    private UUID qualityRuleSetId;

    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum JobStatus {
        QUEUED, PROCESSING, COMPLETED, FAILED, QUALITY_BLOCKED
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
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

    public UUID getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(UUID ruleSetId) {
        this.ruleSetId = ruleSetId;
    }

    public String getRuleIds() {
        return ruleIds;
    }

    public void setRuleIds(String ruleIds) {
        this.ruleIds = ruleIds;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getProcessedRecords() {
        return processedRecords;
    }

    public void setProcessedRecords(int processedRecords) {
        this.processedRecords = processedRecords;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public String getResultData() {
        return resultData;
    }

    public void setResultData(String resultData) {
        this.resultData = resultData;
    }

    public String getErrorLog() {
        return errorLog;
    }

    public void setErrorLog(String errorLog) {
        this.errorLog = errorLog;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getQualityReportId() {
        return qualityReportId;
    }

    public void setQualityReportId(UUID qualityReportId) {
        this.qualityReportId = qualityReportId;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }

    public String getQualityStatus() {
        return qualityStatus;
    }

    public void setQualityStatus(String qualityStatus) {
        this.qualityStatus = qualityStatus;
    }

    public boolean isSkipQualityCheck() {
        return skipQualityCheck;
    }

    public void setSkipQualityCheck(boolean skipQualityCheck) {
        this.skipQualityCheck = skipQualityCheck;
    }

    public UUID getQualityRuleSetId() {
        return qualityRuleSetId;
    }

    public void setQualityRuleSetId(UUID qualityRuleSetId) {
        this.qualityRuleSetId = qualityRuleSetId;
    }

    public String getRuleApplicationErrorsJson() {
        return ruleApplicationErrorsJson;
    }

    public void setRuleApplicationErrorsJson(String ruleApplicationErrorsJson) {
        this.ruleApplicationErrorsJson = ruleApplicationErrorsJson;
    }
}

