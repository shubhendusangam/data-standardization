package com.datastd.quality.dto;

import java.util.UUID;

/**
 * Request body for POST /api/quality/rules/suggest.
 */
public class SuggestRequest {

    private UUID datasetId;

    public SuggestRequest() {}

    public UUID getDatasetId() { return datasetId; }
    public void setDatasetId(UUID datasetId) { this.datasetId = datasetId; }
}

