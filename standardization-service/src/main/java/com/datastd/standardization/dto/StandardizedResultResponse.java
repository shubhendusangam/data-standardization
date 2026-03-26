package com.datastd.standardization.dto;

import com.datastd.common.dto.QualityReport;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class StandardizedResultResponse {

    private UUID jobId;
    private UUID datasetId;
    private String status;
    private int totalRecords;
    private List<Map<String, Object>> standardizedRecords;
    private String errorLog;
    private Boolean qualityBlocked;
    private QualityReport qualityReport;


    public StandardizedResultResponse() {}

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

    public List<Map<String, Object>> getStandardizedRecords() {
        return standardizedRecords;
    }

    public void setStandardizedRecords(List<Map<String, Object>> standardizedRecords) {
        this.standardizedRecords = standardizedRecords;
    }

    public String getErrorLog() {
        return errorLog;
    }

    public void setErrorLog(String errorLog) {
        this.errorLog = errorLog;
    }

    public Boolean getQualityBlocked() {
        return qualityBlocked;
    }

    public void setQualityBlocked(Boolean qualityBlocked) {
        this.qualityBlocked = qualityBlocked;
    }

    public QualityReport getQualityReport() {
        return qualityReport;
    }

    public void setQualityReport(QualityReport qualityReport) {
        this.qualityReport = qualityReport;
    }
}

