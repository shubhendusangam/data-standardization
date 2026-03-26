package com.datastd.standardization.service.impl;

import com.datastd.common.dto.IngestedDatasetResponse;
import com.datastd.common.dto.OverallStatus;
import com.datastd.common.dto.PagedResult;
import com.datastd.common.dto.QualityReport;
import com.datastd.common.dto.RuleResponse;
import com.datastd.common.dto.RuleSetResponse;
import com.datastd.common.dto.ValidationRuleResult;
import com.datastd.standardization.client.DataQualityClient;
import com.datastd.standardization.client.IngestionServiceClient;
import com.datastd.standardization.client.RuleEngineClient;
import com.datastd.standardization.dto.ProcessingRequest;
import com.datastd.standardization.dto.ProcessingStatusResponse;
import com.datastd.standardization.dto.QualityValidateRequest;
import com.datastd.standardization.dto.StandardizedResultResponse;
import com.datastd.standardization.entity.ProcessingJob;
import com.datastd.standardization.entity.ProcessingJob.JobStatus;
import com.datastd.standardization.exception.ResourceNotFoundException;
import com.datastd.standardization.repository.ProcessingJobRepository;
import com.datastd.standardization.service.AsyncJobProcessor;
import com.datastd.standardization.service.StandardizationService;
import com.datastd.standardization.service.rules.RuleApplicationResult;
import com.datastd.standardization.service.rules.RuleExecutionEngine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StandardizationServiceImpl implements StandardizationService {

    private static final Logger log = LoggerFactory.getLogger(StandardizationServiceImpl.class);

    private final ProcessingJobRepository jobRepository;
    private final IngestionServiceClient ingestionClient;
    private final RuleEngineClient ruleEngineClient;
    private final DataQualityClient dataQualityClient;
    private final RuleExecutionEngine ruleExecutionEngine;
    private final ObjectMapper objectMapper;
    private final AsyncJobProcessor asyncJobProcessor;

    public StandardizationServiceImpl(ProcessingJobRepository jobRepository,
                                      IngestionServiceClient ingestionClient,
                                      RuleEngineClient ruleEngineClient,
                                      DataQualityClient dataQualityClient,
                                      RuleExecutionEngine ruleExecutionEngine,
                                      ObjectMapper objectMapper,
                                      AsyncJobProcessor asyncJobProcessor) {
        this.jobRepository = jobRepository;
        this.ingestionClient = ingestionClient;
        this.ruleEngineClient = ruleEngineClient;
        this.dataQualityClient = dataQualityClient;
        this.ruleExecutionEngine = ruleExecutionEngine;
        this.objectMapper = objectMapper;
        this.asyncJobProcessor = asyncJobProcessor;
    }

    @Override
    public ProcessingStatusResponse submitJob(ProcessingRequest request) {
        log.info("Submitting standardization job: datasetId={}, ruleSetId={}, ruleIds={}",
                request.getDatasetId(), request.getRuleSetId(),
                request.getRuleIds() != null ? request.getRuleIds().size() : 0);
        validateRequest(request);

        // Resolve rule IDs
        List<UUID> resolvedRuleIds = resolveRuleIds(request);
        log.debug("Resolved {} rule IDs for job", resolvedRuleIds.size());

        // Create job
        ProcessingJob job = new ProcessingJob();
        job.setDatasetId(request.getDatasetId());
        job.setRuleSetId(request.getRuleSetId());
        job.setSkipQualityCheck(request.isSkipQualityCheck());
        job.setQualityRuleSetId(request.getQualityRuleSetId());
        job.setStatus(JobStatus.QUEUED);

        try {
            job.setRuleIds(objectMapper.writeValueAsString(resolvedRuleIds));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize rule IDs", e);
        }

        ProcessingJob saved = jobRepository.save(job);
        log.info("Job queued: jobId={}, datasetId={}, ruleCount={}, skipQualityCheck={}",
                saved.getId(), saved.getDatasetId(), resolvedRuleIds.size(), saved.isSkipQualityCheck());

        // Delegate to a SEPARATE bean so @Async goes through the Spring AOP proxy.
        // (Self-invocation within this class would bypass the proxy → run synchronously.)
        asyncJobProcessor.processJobAsync(saved.getId());

        return ProcessingStatusResponse.fromEntity(saved);
    }

    @Override
    public List<ProcessingStatusResponse> getAllJobs(JobStatus status, UUID datasetId) {
        log.debug("Listing jobs: status={}, datasetId={}", status, datasetId);
        List<ProcessingJob> jobs;

        if (status != null) {
            jobs = jobRepository.findByStatus(status);
        } else if (datasetId != null) {
            jobs = jobRepository.findByDatasetId(datasetId);
        } else {
            jobs = jobRepository.findAllByOrderByCreatedAtDesc();
        }

        return jobs.stream()
                .map(ProcessingStatusResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public ProcessingStatusResponse getJobStatus(UUID jobId) {
        log.debug("Fetching job status: jobId={}", jobId);
        ProcessingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> {
                    log.warn("Job not found: jobId={}", jobId);
                    return new ResourceNotFoundException("Job not found with id: " + jobId);
                });
        return ProcessingStatusResponse.fromEntity(job);
    }

    @Override
    public StandardizedResultResponse getJobResult(UUID jobId) {
        log.debug("Fetching job result: jobId={}", jobId);
        ProcessingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> {
                    log.warn("Job not found: jobId={}", jobId);
                    return new ResourceNotFoundException("Job not found with id: " + jobId);
                });

        StandardizedResultResponse response = new StandardizedResultResponse();
        response.setJobId(job.getId());
        response.setDatasetId(job.getDatasetId());
        response.setStatus(job.getStatus().name());
        response.setTotalRecords(job.getProcessedRecords());
        response.setErrorLog(job.getErrorLog());

        if (job.getResultData() != null) {
            try {
                List<Map<String, Object>> results = objectMapper.readValue(
                        job.getResultData(), new TypeReference<List<Map<String, Object>>>() {});
                response.setStandardizedRecords(results);
            } catch (JsonProcessingException e) {
                response.setStandardizedRecords(Collections.emptyList());
            }
        }

        return response;
    }

    @Override
    public PagedResult getJobResult(UUID jobId, int page, int size) {
        log.debug("Fetching paginated job result: jobId={}, page={}, size={}", jobId, page, size);
        ProcessingJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> {
                    log.warn("Job not found: jobId={}", jobId);
                    return new ResourceNotFoundException("Job not found with id: " + jobId);
                });

        if (job.getResultData() != null) {
            try {
                List<Map<String, Object>> allResults = objectMapper.readValue(
                        job.getResultData(), new TypeReference<List<Map<String, Object>>>() {});
                return PagedResult.of(allResults, page, size);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse result data for job {}", jobId, e);
                return PagedResult.of(Collections.emptyList(), page, size);
            }
        }

        return PagedResult.of(Collections.emptyList(), page, size);
    }

    @Override
    public StandardizedResultResponse preview(ProcessingRequest request, int maxRecords) {
        log.info("Preview requested: datasetId={}, maxRecords={}", request.getDatasetId(), maxRecords);
        validateRequest(request);
        List<UUID> resolvedRuleIds = resolveRuleIds(request);

        // ── Quality gate for preview ────────────────────────────────
        QualityReport qualityReport = runPreviewQualityCheck(request);

        if (qualityReport != null
                && qualityReport.getOverallStatus() == OverallStatus.FAIL
                && !request.isSkipQualityCheck()) {

            List<String> failedRules = qualityReport.getRuleResults() != null
                    ? qualityReport.getRuleResults().stream()
                        .filter(r -> !r.isPassed())
                        .map(ValidationRuleResult::getRuleName)
                        .collect(Collectors.toList())
                    : Collections.emptyList();

            log.warn("Preview blocked by quality gate: datasetId={}, qualityScore={}, failedRules={}",
                    request.getDatasetId(), qualityReport.getQualityScore(), failedRules);

            StandardizedResultResponse response = new StandardizedResultResponse();
            response.setDatasetId(request.getDatasetId());
            response.setStatus("QUALITY_BLOCKED");
            response.setQualityBlocked(true);
            response.setQualityReport(qualityReport);
            response.setStandardizedRecords(null);
            return response;
        }
        // ── End quality gate ─────────────────────────────────────────

        // Fetch dataset
        log.debug("Fetching dataset from ingestion-service: datasetId={}", request.getDatasetId());
        IngestedDatasetResponse dataset = ingestionClient.getDatasetById(request.getDatasetId());

        try {
            List<Map<String, Object>> records = objectMapper.readValue(
                    dataset.getRawData(), new TypeReference<List<Map<String, Object>>>() {});

            // Limit to maxRecords
            List<Map<String, Object>> subset = records.stream()
                    .limit(maxRecords)
                    .collect(Collectors.toList());

            // Fetch rules
            List<RuleResponse> rules = ruleEngineClient.getRulesByIds(resolvedRuleIds);
            rules.sort(Comparator.comparingInt(RuleResponse::getPriority));

            // Apply rules
            List<Map<String, Object>> standardized = new ArrayList<>();
            for (Map<String, Object> record : subset) {
                RuleApplicationResult ruleResult = ruleExecutionEngine.applyRulesToRecord(record, rules);
                standardized.add(ruleResult.getRecord());
            }

            log.info("Preview complete: datasetId={}, recordsProcessed={}", request.getDatasetId(), standardized.size());

            StandardizedResultResponse response = new StandardizedResultResponse();
            response.setDatasetId(request.getDatasetId());
            response.setStatus("PREVIEW");
            response.setTotalRecords(standardized.size());
            response.setStandardizedRecords(standardized);
            response.setQualityBlocked(false);
            response.setQualityReport(qualityReport);
            return response;

        } catch (JsonProcessingException e) {
            log.error("Failed to parse dataset records: datasetId={}", request.getDatasetId(), e);
            throw new IllegalStateException("Failed to parse dataset records", e);
        }
    }

    /**
     * Runs quality validation for the preview endpoint.
     * Returns QualityReport or null if unavailable (graceful degradation).
     */
    private QualityReport runPreviewQualityCheck(ProcessingRequest request) {
        if (request.getQualityRuleSetId() == null) {
            return null;
        }
        try {
            UUID ruleSetId = request.getQualityRuleSetId();
            log.info("Quality check started: jobId=preview, datasetId={}, ruleSetId={}",
                    request.getDatasetId(), ruleSetId);

            QualityValidateRequest validateRequest = new QualityValidateRequest(request.getDatasetId(), ruleSetId);
            QualityReport report = dataQualityClient.validate(validateRequest);

            if (report.getOverallStatus() == OverallStatus.PASS) {
                log.info("Quality check passed: jobId=preview, qualityScore={}, status=PASS",
                        report.getQualityScore());
            } else if (report.getOverallStatus() == OverallStatus.WARN) {
                List<String> failedRules = report.getRuleResults() != null
                        ? report.getRuleResults().stream()
                            .filter(r -> !r.isPassed())
                            .map(ValidationRuleResult::getRuleName)
                            .collect(Collectors.toList())
                        : Collections.emptyList();
                log.warn("Quality check warnings: jobId=preview, qualityScore={}, failedRules={}",
                        report.getQualityScore(), failedRules);
            }

            return report;
        } catch (Exception e) {
            log.warn("Quality check unavailable for preview datasetId={}, proceeding without gate: {}",
                    request.getDatasetId(), e.getMessage());
            return null;
        }
    }

    private void validateRequest(ProcessingRequest request) {
        if (request.getRuleSetId() == null &&
                (request.getRuleIds() == null || request.getRuleIds().isEmpty())) {
            throw new IllegalArgumentException("Either ruleSetId or ruleIds must be provided");
        }
    }

    private List<UUID> resolveRuleIds(ProcessingRequest request) {
        if (request.getRuleSetId() != null) {
            RuleSetResponse ruleSet = ruleEngineClient.getRuleSetById(request.getRuleSetId());
            return ruleSet.getRuleIds();
        }
        return request.getRuleIds();
    }
}

