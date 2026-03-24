package com.datastd.standardization.controller;

import com.datastd.standardization.dto.ProcessingRequest;
import com.datastd.standardization.dto.ProcessingStatusResponse;
import com.datastd.standardization.dto.StandardizedResultResponse;
import com.datastd.standardization.entity.ProcessingJob;
import com.datastd.standardization.service.StandardizationService;
import jakarta.validation.Valid;
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
    public ResponseEntity<StandardizedResultResponse> getJobResult(@PathVariable UUID jobId) {
        StandardizedResultResponse response = standardizationService.getJobResult(jobId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/preview")
    public ResponseEntity<StandardizedResultResponse> preview(
            @Valid @RequestBody ProcessingRequest request,
            @RequestParam(defaultValue = "5") int maxRecords) {
        StandardizedResultResponse response = standardizationService.preview(request, maxRecords);
        return ResponseEntity.ok(response);
    }
}

