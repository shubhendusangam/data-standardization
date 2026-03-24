package com.datastd.ingestion.dto;

import com.datastd.ingestion.entity.IngestedDataset;

import java.time.LocalDateTime;
import java.util.UUID;

public class DatasetUploadResponse {

    private UUID datasetId;
    private String name;
    private String sourceType;
    private int recordCount;
    private String status;
    private String message;
    private LocalDateTime createdAt;

    public DatasetUploadResponse() {}

    public DatasetUploadResponse(IngestedDataset dataset, String message) {
        this.datasetId = dataset.getId();
        this.name = dataset.getName();
        this.sourceType = dataset.getSourceType().name();
        this.recordCount = dataset.getRecordCount();
        this.status = dataset.getStatus().name();
        this.createdAt = dataset.getCreatedAt();
        this.message = message;
    }

    // Getters and Setters

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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

