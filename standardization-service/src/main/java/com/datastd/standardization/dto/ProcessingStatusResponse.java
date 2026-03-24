package com.datastd.standardization.dto;

import com.datastd.standardization.entity.ProcessingJob;

import java.time.LocalDateTime;
import java.util.UUID;

public class ProcessingStatusResponse {

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
}

