package com.datastd.ingestion.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class DatasetSummaryResponse {

    private UUID datasetId;
    private String name;
    private String sourceType;
    private int recordCount;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DatasetSummaryResponse() {}

    public DatasetSummaryResponse(UUID datasetId, String name, String sourceType,
                                  int recordCount, String status,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.datasetId = datasetId;
        this.name = name;
        this.sourceType = sourceType;
        this.recordCount = recordCount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(UUID datasetId) {
        this.datasetId = datasetId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

