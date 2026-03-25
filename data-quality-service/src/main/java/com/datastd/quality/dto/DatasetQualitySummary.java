package com.datastd.quality.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Summary of the latest quality state for a single dataset.
 */
public class DatasetQualitySummary {

    private UUID datasetId;
    private int latestScore;
    private String latestStatus;
    private Instant lastEvaluatedAt;
    private String trend; // IMPROVING, STABLE, DEGRADING, UNKNOWN

    public DatasetQualitySummary() {}

    public UUID getDatasetId() { return datasetId; }
    public void setDatasetId(UUID datasetId) { this.datasetId = datasetId; }
    public int getLatestScore() { return latestScore; }
    public void setLatestScore(int latestScore) { this.latestScore = latestScore; }
    public String getLatestStatus() { return latestStatus; }
    public void setLatestStatus(String latestStatus) { this.latestStatus = latestStatus; }
    public Instant getLastEvaluatedAt() { return lastEvaluatedAt; }
    public void setLastEvaluatedAt(Instant lastEvaluatedAt) { this.lastEvaluatedAt = lastEvaluatedAt; }
    public String getTrend() { return trend; }
    public void setTrend(String trend) { this.trend = trend; }
}

