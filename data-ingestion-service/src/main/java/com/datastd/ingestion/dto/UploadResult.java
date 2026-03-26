package com.datastd.ingestion.dto;

import com.datastd.ingestion.entity.IngestedDataset;

/**
 * Wraps the result of a file upload: the persisted dataset and the parse metadata.
 * Eliminates the need for a ThreadLocal to pass parse results from service to controller.
 */
public class UploadResult {

    private final IngestedDataset dataset;
    private final FileParseResult parseResult;

    public UploadResult(IngestedDataset dataset, FileParseResult parseResult) {
        this.dataset = dataset;
        this.parseResult = parseResult;
    }

    public IngestedDataset getDataset() {
        return dataset;
    }

    public FileParseResult getParseResult() {
        return parseResult;
    }
}

