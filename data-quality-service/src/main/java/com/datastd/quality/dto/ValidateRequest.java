package com.datastd.quality.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request body for POST /api/quality/validate.
 * Either provide a list of ruleIds, or a ruleSetId (not both).
 */
public class ValidateRequest {

    private UUID datasetId;
    private List<UUID> ruleIds;
    private UUID ruleSetId;

    // Getters and Setters

    public UUID getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(UUID datasetId) {
        this.datasetId = datasetId;
    }

    public List<UUID> getRuleIds() {
        return ruleIds;
    }

    public void setRuleIds(List<UUID> ruleIds) {
        this.ruleIds = ruleIds;
    }

    public UUID getRuleSetId() {
        return ruleSetId;
    }

    public void setRuleSetId(UUID ruleSetId) {
        this.ruleSetId = ruleSetId;
    }
}

