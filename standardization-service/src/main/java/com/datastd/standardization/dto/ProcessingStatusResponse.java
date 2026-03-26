package com.datastd.standardization.dto;

import com.datastd.common.dto.RuleApplicationError;
import com.datastd.standardization.entity.ProcessingJob;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ProcessingStatusResponse {

    private static final int MAX_ERRORS_IN_RESPONSE = 50;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private UUID jobId;
    private UUID datasetId;
    private String status;
    private int totalRecords;
    private int processedRecords;
    private int errorCount;
    private double progressPercentage;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private Integer qualityScore;
    private String qualityStatus;
    private UUID qualityReportId;
    private String errorLog;
    private List<RuleApplicationError> ruleApplicationErrors;
    private boolean errorsTruncated;

    public ProcessingStatusResponse() {}

    public static ProcessingStatusResponse fromEntity(ProcessingJob job) {
        ProcessingStatusResponse response = new ProcessingStatusResponse();
        response.setJobId(job.getId());
        response.setDatasetId(job.getDatasetId());
        response.setStatus(job.getStatus().name());
        response.setTotalRecords(job.getTotalRecords());
        response.setProcessedRecords(job.getProcessedRecords());
        response.setErrorCount(job.getErrorCount());
        response.setStartedAt(job.getStartedAt());
        response.setCompletedAt(job.getCompletedAt());
        response.setCreatedAt(job.getCreatedAt());
        response.setQualityScore(job.getQualityScore());
        response.setQualityStatus(job.getQualityStatus());
        response.setQualityReportId(job.getQualityReportId());
        response.setErrorLog(job.getErrorLog());

        // Deserialize and cap rule application errors
        if (job.getRuleApplicationErrorsJson() != null && !job.getRuleApplicationErrorsJson().isEmpty()) {
            try {
                List<RuleApplicationError> allErrors = MAPPER.readValue(
                        job.getRuleApplicationErrorsJson(),
                        new TypeReference<List<RuleApplicationError>>() {});
                if (allErrors.size() > MAX_ERRORS_IN_RESPONSE) {
                    response.setRuleApplicationErrors(allErrors.subList(0, MAX_ERRORS_IN_RESPONSE));
                    response.setErrorsTruncated(true);
                } else {
                    response.setRuleApplicationErrors(allErrors);
                    response.setErrorsTruncated(false);
                }
            } catch (Exception e) {
                response.setRuleApplicationErrors(Collections.emptyList());
                response.setErrorsTruncated(false);
            }
        } else {
            response.setRuleApplicationErrors(Collections.emptyList());
            response.setErrorsTruncated(false);
        }

        if (job.getTotalRecords() > 0) {
            response.setProgressPercentage(
                    (double) job.getProcessedRecords() / job.getTotalRecords() * 100.0);
        } else {
            response.setProgressPercentage(0);
        }

        return response;
    }

    // Getters and Setters

    public UUID getJobId() {
        return jobId;
    }

    public void setJobId(UUID jobId) {
        this.jobId = jobId;
    }

    public UUID getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(UUID datasetId) {
        this.datasetId = datasetId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public double getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(double progressPercentage) {
        this.progressPercentage = progressPercentage;
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

    public UUID getQualityReportId() {
        return qualityReportId;
    }

    public void setQualityReportId(UUID qualityReportId) {
        this.qualityReportId = qualityReportId;
    }

    public String getErrorLog() {
        return errorLog;
    }

    public void setErrorLog(String errorLog) {
        this.errorLog = errorLog;
    }

    public List<RuleApplicationError> getRuleApplicationErrors() {
        return ruleApplicationErrors;
    }

    public void setRuleApplicationErrors(List<RuleApplicationError> ruleApplicationErrors) {
        this.ruleApplicationErrors = ruleApplicationErrors;
    }

    public boolean isErrorsTruncated() {
        return errorsTruncated;
    }

    public void setErrorsTruncated(boolean errorsTruncated) {
        this.errorsTruncated = errorsTruncated;
    }
}

