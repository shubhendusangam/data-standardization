package com.datastd.standardization.controller;

import com.datastd.common.dto.PagedResult;
import com.datastd.standardization.dto.ProcessingRequest;
import com.datastd.standardization.dto.ProcessingStatusResponse;
import com.datastd.standardization.dto.StandardizedResultResponse;
import com.datastd.standardization.entity.ProcessingJob;
import com.datastd.standardization.service.StandardizationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/standardization")
public class StandardizationController {

    private final StandardizationService standardizationService;

    public StandardizationController(StandardizationService standardizationService) {
        this.standardizationService = standardizationService;
    }

    @PostMapping("/process")
    public ResponseEntity<ProcessingStatusResponse> submitJob(@Valid @RequestBody ProcessingRequest request) {
        ProcessingStatusResponse response = standardizationService.submitJob(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/jobs")
    public ResponseEntity<List<ProcessingStatusResponse>> getAllJobs(
            @RequestParam(required = false) ProcessingJob.JobStatus status,
            @RequestParam(required = false) UUID datasetId) {
        List<ProcessingStatusResponse> jobs = standardizationService.getAllJobs(status, datasetId);
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<ProcessingStatusResponse> getJobStatus(@PathVariable UUID jobId) {
        ProcessingStatusResponse response = standardizationService.getJobStatus(jobId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/jobs/{jobId}/result")
    public ResponseEntity<PagedResult> getJobResult(
            @PathVariable UUID jobId,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "100") int size) {
        if (size > 1000) {
            throw new IllegalArgumentException("size must not exceed 1000");
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative");
        }
        PagedResult result = standardizationService.getJobResult(jobId, page, size);

        // Content-Range: records 0-99/4820
        long total = result.getTotalRecords();
        int from = page * size;
        int to = from + result.getRecords().size() - 1;
        String contentRange = (total == 0)
                ? "records */0"
                : "records " + from + "-" + to + "/" + total;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Range", contentRange);

        return ResponseEntity.ok().headers(headers).body(result);
    }

    @PostMapping("/preview")
    public ResponseEntity<StandardizedResultResponse> preview(
            @Valid @RequestBody ProcessingRequest request,
            @RequestParam(defaultValue = "5") int maxRecords) {
        StandardizedResultResponse response = standardizationService.preview(request, maxRecords);
        return ResponseEntity.ok(response);
    }
}

