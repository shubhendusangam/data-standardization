package com.datastd.ingestion.controller;

import com.datastd.common.dto.IngestedDatasetResponse;
import com.datastd.ingestion.dto.DatasetSummaryResponse;
import com.datastd.ingestion.dto.DatasetUploadResponse;
import com.datastd.ingestion.dto.JsonDataRequest;
import com.datastd.ingestion.dto.UploadResult;
import com.datastd.ingestion.entity.IngestedDataset;
import com.datastd.ingestion.exception.FileSizeLimitExceededException;
import com.datastd.ingestion.exception.UnsupportedFileTypeException;
import com.datastd.ingestion.mapper.DatasetMapper;
import com.datastd.ingestion.service.IngestionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
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

    private final long maxFileSizeBytes;
    private final List<String> supportedTypes;

    public IngestionController(
            IngestionService ingestionService,
            @Value("${datastandard.ingestion.max-file-size-bytes:52428800}") long maxFileSizeBytes,
            @Value("${datastandard.ingestion.supported-types:text/csv,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet}")
            List<String> supportedTypes) {
        this.ingestionService = ingestionService;
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.supportedTypes = supportedTypes;
    }

    @PostMapping("/upload")
    public ResponseEntity<DatasetUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        if (file.getSize() > maxFileSizeBytes) {
            throw new FileSizeLimitExceededException(
                    "File size " + formatSize(file.getSize()) + " exceeds the maximum allowed size of " + formatSize(maxFileSizeBytes),
                    file.getSize(), maxFileSizeBytes);
        }

        String contentType = file.getContentType();
        if (contentType == null || !supportedTypes.contains(contentType)) {
            throw new UnsupportedFileTypeException(
                    "Unsupported file type: " + contentType + ". Allowed: CSV, XLS, XLSX");
        }

        UploadResult uploadResult = ingestionService.uploadFile(file);
        IngestedDataset dataset = uploadResult.getDataset();

        if (dataset.getStatus() == IngestedDataset.DatasetStatus.FAILED) {
            DatasetUploadResponse response = new DatasetUploadResponse(dataset,
                    "File upload failed: could not parse file");
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
        DatasetUploadResponse response = new DatasetUploadResponse(dataset,
                "File uploaded and parsed successfully", uploadResult.getParseResult());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/json")
    public ResponseEntity<DatasetUploadResponse> ingestJson(@Valid @RequestBody JsonDataRequest request) {
        IngestedDataset dataset = ingestionService.ingestJsonData(request);
        if (dataset.getStatus() == IngestedDataset.DatasetStatus.FAILED) {
            DatasetUploadResponse response = new DatasetUploadResponse(dataset,
                    "JSON data ingestion failed");
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
        }
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

    // ── helpers ──────────────────────────────────────────────────

    /**
     * Format byte size to a human-readable string (e.g. "75.3 MB").
     */
    static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double mb = bytes / (1024.0 * 1024.0);
        if (mb < 1.0) {
            double kb = bytes / 1024.0;
            return String.format("%.1f KB", kb);
        }
        return String.format("%.1f MB", mb);
    }
}

