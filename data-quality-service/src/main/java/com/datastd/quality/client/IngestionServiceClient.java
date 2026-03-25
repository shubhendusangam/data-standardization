package com.datastd.quality.client;

import com.datastd.common.dto.IngestedDatasetResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "data-ingestion-service")
public interface IngestionServiceClient {

    @GetMapping("/api/ingestion/datasets/{id}")
    IngestedDatasetResponse getDatasetById(@PathVariable("id") UUID id);
}

