package com.datastd.ingestion.mapper;

import com.datastd.common.dto.IngestedDatasetResponse;
import com.datastd.ingestion.dto.FileParseResult;
import com.datastd.ingestion.entity.IngestedDataset;

/**
 * Maps IngestedDataset entity to the shared IngestedDatasetResponse DTO
 * from common-dto, keeping entity-to-DTO conversion logic local to this service.
 */
public final class DatasetMapper {

    private DatasetMapper() {}

    public static IngestedDatasetResponse toResponse(IngestedDataset entity) {
        return toResponse(entity, null);
    }

    public static IngestedDatasetResponse toResponse(IngestedDataset entity, FileParseResult parseResult) {
        IngestedDatasetResponse dto = new IngestedDatasetResponse();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSourceType(entity.getSourceType().name());
        dto.setStatus(entity.getStatus().name());
        dto.setRecordCount(entity.getRecordCount());
        dto.setRawData(entity.getRawData());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        if (parseResult != null) {
            dto.setParsedRowCount(parseResult.getParsedRowCount());
            dto.setSkippedRowCount(parseResult.getSkippedRowCount());
            dto.setParseWarnings(parseResult.getWarnings());
            dto.setWarningsTruncated(parseResult.isWarningsTruncated());
        } else {
            dto.setParsedRowCount(entity.getRecordCount());
            dto.setSkippedRowCount(0);
        }

        return dto;
    }
}

