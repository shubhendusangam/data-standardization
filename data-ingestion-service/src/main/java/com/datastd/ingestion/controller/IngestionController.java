package com.datastd.ingestion.controller;

import com.datastd.common.dto.IngestedDatasetResponse;
import com.datastd.ingestion.dto.DatasetSummaryResponse;
import com.datastd.ingestion.dto.DatasetUploadResponse;
import com.datastd.ingestion.dto.JsonDataRequest;
import com.datastd.ingestion.entity.IngestedDataset;
import com.datastd.ingestion.mapper.DatasetMapper;
import com.datastd.ingestion.service.IngestionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ingestion")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/upload")
    public ResponseEntity<DatasetUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        IngestedDataset dataset = ingestionService.uploadFile(file);
        DatasetUploadResponse response = new DatasetUploadResponse(dataset,
                "File uploaded and parsed successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/json")
    public ResponseEntity<DatasetUploadResponse> ingestJson(@Valid @RequestBody JsonDataRequest request) {
        IngestedDataset dataset = ingestionService.ingestJsonData(request);
        DatasetUploadResponse response = new DatasetUploadResponse(dataset,
                "JSON data ingested successfully");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/datasets")
    public ResponseEntity<List<DatasetSummaryResponse>> getAllDatasets() {
        List<IngestedDataset> datasets = ingestionService.getAllDatasets();
        List<DatasetSummaryResponse> summaries = datasets.stream()
                .map(d -> new DatasetSummaryResponse(
                        d.getId(), d.getName(), d.getSourceType().name(),
                        d.getRecordCount(), d.getStatus().name(),
                        d.getCreatedAt(), d.getUpdatedAt()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/datasets/{id}")
    public ResponseEntity<IngestedDatasetResponse> getDatasetById(@PathVariable UUID id) {
        IngestedDataset dataset = ingestionService.getDatasetById(id);
        return ResponseEntity.ok(DatasetMapper.toResponse(dataset));
    }

    @DeleteMapping("/datasets/{id}")
    public ResponseEntity<Void> deleteDataset(@PathVariable UUID id) {
        ingestionService.deleteDataset(id);
        return ResponseEntity.noContent().build();
    }
}

