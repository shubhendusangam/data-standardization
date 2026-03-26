package com.datastd.common.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared DTO representing an ingested dataset.
 * Used by the Feign client in standardization-service and returned
 * by data-ingestion-service's REST API.
 */
public class IngestedDatasetResponse {

    private UUID id;
    private String name;
    private String sourceType;
    private String status;
    private int recordCount;
    private String rawData;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private int parsedRowCount;
    private int skippedRowCount;
    private List<ParseWarning> parseWarnings = new ArrayList<>();
    private boolean warningsTruncated;

    public IngestedDatasetResponse() {}

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getRecordCount() {
        return recordCount;
    }

    public void setRecordCount(int recordCount) {
        this.recordCount = recordCount;
    }

    public String getRawData() {
        return rawData;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
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

