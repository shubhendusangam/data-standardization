package com.datastd.ingestion.dto;

import com.datastd.common.dto.ParseWarning;
import com.datastd.ingestion.entity.IngestedDataset;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatasetUploadResponse {

    private UUID datasetId;
    private String name;
    private String sourceType;
    private int recordCount;
    private String status;
    private String message;
    private LocalDateTime createdAt;

    private int parsedRowCount;
    private int skippedRowCount;
    private List<ParseWarning> parseWarnings = new ArrayList<>();
    private boolean warningsTruncated;

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

    public DatasetUploadResponse(IngestedDataset dataset, String message, FileParseResult parseResult) {
        this(dataset, message);
        if (parseResult != null) {
            this.parsedRowCount = parseResult.getParsedRowCount();
            this.skippedRowCount = parseResult.getSkippedRowCount();
            this.parseWarnings = parseResult.getWarnings();
            this.warningsTruncated = parseResult.isWarningsTruncated();
        } else {
            this.parsedRowCount = dataset.getRecordCount();
            this.skippedRowCount = 0;
        }
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

    public int getParsedRowCount() {
        return parsedRowCount;
    }

    public void setParsedRowCount(int parsedRowCount) {
        this.parsedRowCount = parsedRowCount;
    }

    public int getSkippedRowCount() {
        return skippedRowCount;
    }

    public void setSkippedRowCount(int skippedRowCount) {
        this.skippedRowCount = skippedRowCount;
    }

    public List<ParseWarning> getParseWarnings() {
        return parseWarnings;
    }

    public void setParseWarnings(List<ParseWarning> parseWarnings) {
        this.parseWarnings = parseWarnings;
    }

    public boolean isWarningsTruncated() {
        return warningsTruncated;
    }

    public void setWarningsTruncated(boolean warningsTruncated) {
        this.warningsTruncated = warningsTruncated;
    }
}

