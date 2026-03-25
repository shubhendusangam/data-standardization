package com.datastd.standardization.dto;

import java.util.UUID;

/**
 * Request body for POST /api/quality/validate via Feign.
 * Mirrors the data-quality-service's ValidateRequest fields.
 */
public class QualityValidateRequest {

    private UUID datasetId;
    private UUID ruleSetId;

    public QualityValidateRequest() {}

    public QualityValidateRequest(UUID datasetId, UUID ruleSetId) {
        this.datasetId = datasetId;
        this.ruleSetId = ruleSetId;
    }

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
}

