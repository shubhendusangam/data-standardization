package com.datastd.standardization.service;

import com.datastd.standardization.dto.ProcessingRequest;
import com.datastd.standardization.dto.ProcessingStatusResponse;
import com.datastd.standardization.dto.StandardizedResultResponse;
import com.datastd.standardization.entity.ProcessingJob;

import java.util.List;
import java.util.UUID;

public interface StandardizationService {
    ProcessingStatusResponse submitJob(ProcessingRequest request);
    List<ProcessingStatusResponse> getAllJobs(ProcessingJob.JobStatus status, UUID datasetId);
    ProcessingStatusResponse getJobStatus(UUID jobId);
    StandardizedResultResponse getJobResult(UUID jobId);
    StandardizedResultResponse preview(ProcessingRequest request, int maxRecords);
}

