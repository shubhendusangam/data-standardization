package com.datastd.standardization.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class ProcessingRequest {

    @NotNull(message = "Dataset ID is required")
    private UUID datasetId;

    private UUID ruleSetId;

    private List<UUID> ruleIds;

    private boolean skipQualityCheck;

    private UUID qualityRuleSetId;

    public ProcessingRequest() {}

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

    public List<UUID> getRuleIds() {
        return ruleIds;
    }

    public void setRuleIds(List<UUID> ruleIds) {
        this.ruleIds = ruleIds;
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
}

