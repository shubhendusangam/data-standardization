package com.datastd.ingestion.service.impl;

import com.datastd.ingestion.dto.FileParseResult;
import com.datastd.ingestion.dto.JsonDataRequest;
import com.datastd.ingestion.dto.UploadResult;
import com.datastd.ingestion.entity.IngestedDataset;
import com.datastd.ingestion.entity.IngestedDataset.DatasetStatus;
import com.datastd.ingestion.entity.IngestedDataset.SourceType;
import com.datastd.ingestion.exception.ResourceNotFoundException;
import com.datastd.ingestion.repository.IngestedDatasetRepository;
import com.datastd.ingestion.service.FileParserService;
import com.datastd.ingestion.service.IngestionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class IngestionServiceImpl implements IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionServiceImpl.class);

    private final IngestedDatasetRepository repository;
    private final FileParserService fileParserService;
    private final ObjectMapper objectMapper;

    public IngestionServiceImpl(IngestedDatasetRepository repository,
                                FileParserService fileParserService,
                                ObjectMapper objectMapper) {
        this.repository = repository;
        this.fileParserService = fileParserService;
        this.objectMapper = objectMapper;
    }

    @Override
    public UploadResult uploadFile(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        log.info("Uploading file: name={}, size={} bytes", originalFilename, file.getSize());

        if (originalFilename == null || originalFilename.isBlank()) {
            log.warn("Upload rejected: file name is null or blank");
            throw new IllegalArgumentException("File name is required");
        }

        SourceType sourceType = determineSourceType(originalFilename);
        IngestedDataset dataset = new IngestedDataset();
        dataset.setName(originalFilename);
        dataset.setSourceType(sourceType);
        FileParseResult parseResult = null;

        try {
            if (sourceType == SourceType.EXCEL) {
                parseResult = fileParserService.parseExcel(file.getInputStream());
            } else if (sourceType == SourceType.CSV) {
                parseResult = fileParserService.parseCsv(file.getInputStream());
            } else {
                throw new IllegalArgumentException("Unsupported file type: " + originalFilename);
            }

            List<Map<String, String>> records = parseResult.getRecords();
            dataset.setRawData(objectMapper.writeValueAsString(records));
            dataset.setRecordCount(records.size());
            dataset.setStatus(DatasetStatus.PARSED);

            if (parseResult.getSkippedRowCount() > 0) {
                log.warn("File parse completed with warnings: datasetId=pending, parsedRows={}, skippedRows={}",
                        parseResult.getParsedRowCount(), parseResult.getSkippedRowCount());
            }

            log.info("File parsed successfully: name={}, type={}, records={}", originalFilename, sourceType, records.size());

        } catch (IOException e) {
            log.error("Failed to parse file: name={}, error={}", originalFilename, e.getMessage(), e);
            dataset.setStatus(DatasetStatus.FAILED);
            dataset.setRawData("Error: " + e.getMessage());
            dataset.setRecordCount(0);
        }

        IngestedDataset saved = repository.save(dataset);
        log.info("Dataset persisted: id={}, name={}, status={}", saved.getId(), saved.getName(), saved.getStatus());
        return new UploadResult(saved, parseResult);
    }

    @Override
    public IngestedDataset ingestJsonData(JsonDataRequest request) {
        log.info("Ingesting JSON data: name={}, recordCount={}", request.getName(),
                request.getRecords() != null ? request.getRecords().size() : 0);

        if (request.getRecords() == null || request.getRecords().isEmpty()) {
            throw new IllegalArgumentException("Records list cannot be null or empty");
        }

        IngestedDataset dataset = new IngestedDataset();
        dataset.setName(request.getName());
        dataset.setSourceType(SourceType.API);

        try {
            dataset.setRawData(objectMapper.writeValueAsString(request.getRecords()));
            dataset.setRecordCount(request.getRecords().size());
            dataset.setStatus(DatasetStatus.PARSED);
        } catch (JsonProcessingException e) {
            dataset.setStatus(DatasetStatus.FAILED);
            dataset.setRawData("Error: " + e.getMessage());
            dataset.setRecordCount(0);
        }

        IngestedDataset saved = repository.save(dataset);
        log.info("JSON dataset persisted: id={}, name={}, status={}", saved.getId(), saved.getName(), saved.getStatus());
        return saved;
    }

    @Override
    public List<IngestedDataset> getAllDatasets() {
        log.debug("Listing all datasets");
        return repository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public IngestedDataset getDatasetById(UUID id) {
        log.debug("Fetching dataset: id={}", id);
        return repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Dataset not found: id={}", id);
                    return new ResourceNotFoundException("Dataset not found with id: " + id);
                });
    }

    @Override
    public void deleteDataset(UUID id) {
        if (!repository.existsById(id)) {
            log.warn("Delete failed — dataset not found: id={}", id);
            throw new ResourceNotFoundException("Dataset not found with id: " + id);
        }
        repository.deleteById(id);
        log.info("Dataset deleted: id={}", id);
    }

    private SourceType determineSourceType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
            return SourceType.EXCEL;
        } else if (lower.endsWith(".csv")) {
            return SourceType.CSV;
        }
        throw new IllegalArgumentException("Unsupported file format. Supported: .xlsx, .xls, .csv");
    }
}
