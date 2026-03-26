package com.datastd.ingestion.service;

import com.datastd.ingestion.dto.JsonDataRequest;
import com.datastd.ingestion.dto.UploadResult;
import com.datastd.ingestion.entity.IngestedDataset;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface IngestionService {
    UploadResult uploadFile(MultipartFile file);
    IngestedDataset ingestJsonData(JsonDataRequest request);
    List<IngestedDataset> getAllDatasets();
    IngestedDataset getDatasetById(UUID id);
    void deleteDataset(UUID id);
}

