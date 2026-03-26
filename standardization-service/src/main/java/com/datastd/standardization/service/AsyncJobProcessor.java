package com.datastd.standardization.service;

import com.datastd.common.dto.IngestedDatasetResponse;
import com.datastd.common.dto.OverallStatus;
import com.datastd.common.dto.QualityReport;
import com.datastd.common.dto.RuleApplicationError;
import com.datastd.common.dto.RuleResponse;
import com.datastd.common.dto.ValidationRuleResult;
import com.datastd.standardization.client.DataQualityClient;
import com.datastd.standardization.client.IngestionServiceClient;
import com.datastd.standardization.client.RuleEngineClient;
import com.datastd.standardization.dto.QualityValidateRequest;
import com.datastd.standardization.entity.ProcessingJob;
import com.datastd.standardization.entity.ProcessingJob.JobStatus;
import com.datastd.standardization.exception.ResourceNotFoundException;
import com.datastd.standardization.repository.ProcessingJobRepository;
import com.datastd.standardization.service.rules.RuleApplicationResult;
import com.datastd.standardization.service.rules.RuleExecutionEngine;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Separate Spring bean for async job execution.
 * <p>
 * Extracted from StandardizationServiceImpl so that @Async goes through
 * the Spring AOP proxy (self-invocation within the same class would bypass it).
 * <p>
 * Uses the "jobTaskExecutor" thread pool defined in AsyncConfig.
 * When migrating to Kafka, this class becomes the KafkaListener consumer.
 */
@Service
public class AsyncJobProcessor {

    private static final Logger log = LoggerFactory.getLogger(AsyncJobProcessor.class);

    private final ProcessingJobRepository jobRepository;
    private final IngestionServiceClient ingestionClient;
    private final RuleEngineClient ruleEngineClient;
    private final DataQualityClient dataQualityClient;
    private final RuleExecutionEngine ruleExecutionEngine;
    private final ObjectMapper objectMapper;

    public AsyncJobProcessor(ProcessingJobRepository jobRepository,
                             IngestionServiceClient ingestionClient,
                             RuleEngineClient ruleEngineClient,
                             DataQualityClient dataQualityClient,
                             RuleExecutionEngine ruleExecutionEngine,
                             ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.ingestionClient = ingestionClient;
        this.ruleEngineClient = ruleEngineClient;
        this.dataQualityClient = dataQualityClient;
        this.ruleExecutionEngine = ruleExecutionEngine;
        this.objectMapper = objectMapper;
    }

    /**
     * Processes a standardization job asynchronously.
     * The caller (StandardizationServiceImpl.submitJob) returns immediately
     * with status QUEUED; clients poll GET /jobs/{id} for progress.
     */
    @Async("jobTaskExecutor")
    public void processJobAsync(UUID jobId) {
        ProcessingJob job = null;
        try {
            job = jobRepository.findById(jobId)
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + jobId));

            job.setStatus(JobStatus.PROCESSING);
            job.setStartedAt(LocalDateTime.now());
            jobRepository.save(job);

            // Fetch dataset from ingestion-service via Feign
            IngestedDatasetResponse dataset = ingestionClient.getDatasetById(job.getDatasetId());
            List<Map<String, Object>> records = objectMapper.readValue(
                    dataset.getRawData(), new TypeReference<List<Map<String, Object>>>() {});

            job.setTotalRecords(records.size());
            jobRepository.save(job);

            // ── Quality gate ─────────────────────────────────────────
            QualityReport qualityReport = runQualityCheck(job);

            if (qualityReport != null) {
                job.setQualityReportId(qualityReport.getReportId());
                job.setQualityScore(qualityReport.getQualityScore());
                job.setQualityStatus(qualityReport.getOverallStatus().name());

                if (qualityReport.getOverallStatus() == OverallStatus.FAIL && !job.isSkipQualityCheck()) {
                    List<String> failedRules = qualityReport.getRuleResults() != null
                            ? qualityReport.getRuleResults().stream()
                                .filter(r -> !r.isPassed())
                                .map(ValidationRuleResult::getRuleName)
                                .collect(Collectors.toList())
                            : Collections.emptyList();

                    log.warn("Job blocked by quality gate: jobId={}, datasetId={}, qualityScore={}, failedRules={}",
                            jobId, job.getDatasetId(), qualityReport.getQualityScore(), failedRules);

                    job.setStatus(JobStatus.QUALITY_BLOCKED);
                    job.setCompletedAt(LocalDateTime.now());
                    jobRepository.save(job);
                    return;
                }
            }
            // ── End quality gate ─────────────────────────────────────

            // Fetch rules from rule-engine-service via Feign
            List<UUID> ruleIds = objectMapper.readValue(
                    job.getRuleIds(), new TypeReference<List<UUID>>() {});
            List<RuleResponse> rules = ruleEngineClient.getRulesByIds(ruleIds);

            // Sort rules by priority
            rules.sort(Comparator.comparingInt(RuleResponse::getPriority));

            // Apply rules to each record
            List<Map<String, Object>> standardizedRecords = new ArrayList<>();
            List<RuleApplicationError> allRuleErrors = new ArrayList<>();
            StringBuilder errorLog = new StringBuilder();
            int errorCount = 0;
            int processed = 0;

            for (Map<String, Object> record : records) {
                try {
                    RuleApplicationResult ruleResult = ruleExecutionEngine.applyRulesToRecord(record, rules);
                    standardizedRecords.add(ruleResult.getRecord());

                    if (ruleResult.hasErrors()) {
                        errorCount++;
                        for (RuleApplicationError err : ruleResult.getErrors()) {
                            err.setRecordIndex(processed);
                            allRuleErrors.add(err);
                        }
                        errorLog.append("Record ").append(processed + 1)
                                .append(": ").append(ruleResult.getErrors().size())
                                .append(" rule error(s)\n");
                    }
                } catch (Exception e) {
                    errorCount++;
                    errorLog.append("Record ").append(processed + 1)
                            .append(": ").append(e.getMessage()).append("\n");
                    standardizedRecords.add(record); // keep original on error
                }
                processed++;

                // Update progress every 100 records
                if (processed % 100 == 0) {
                    job.setProcessedRecords(processed);
                    jobRepository.save(job);
                }
            }

            if (!allRuleErrors.isEmpty()) {
                log.warn("Job completed with rule errors: jobId={}, totalRecords={}, errorRecords={}",
                        jobId, processed, errorCount);
            }

            // Save results
            job.setProcessedRecords(processed);
            job.setErrorCount(errorCount);
            job.setResultData(objectMapper.writeValueAsString(standardizedRecords));
            job.setErrorLog(errorLog.toString());
            if (!allRuleErrors.isEmpty()) {
                job.setRuleApplicationErrorsJson(objectMapper.writeValueAsString(allRuleErrors));
            }
            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            jobRepository.save(job);

            log.info("Job {} completed: {} records processed, {} errors", jobId, processed, errorCount);

        } catch (Exception e) {
            log.error("Job {} failed: {}", jobId, e.getMessage(), e);
            if (job != null) {
                job.setStatus(JobStatus.FAILED);
                job.setErrorLog("Job failed: " + e.getMessage());
                job.setCompletedAt(LocalDateTime.now());
                jobRepository.save(job);
            }
        }
    }

    /**
     * Runs a quality check for the given job via the data-quality-service.
     * Returns the QualityReport, or null if the quality service is unavailable
     * (graceful degradation — the job proceeds without a quality gate).
     */
    private QualityReport runQualityCheck(ProcessingJob job) {
        try {
            UUID ruleSetId = job.getQualityRuleSetId(); // may be null → basic profiling only
            log.info("Quality check started: jobId={}, datasetId={}, ruleSetId={}",
                    job.getId(), job.getDatasetId(), ruleSetId);

            QualityValidateRequest request = new QualityValidateRequest(job.getDatasetId(), ruleSetId);
            QualityReport report = dataQualityClient.validate(request);

            if (report.getOverallStatus() == OverallStatus.PASS) {
                log.info("Quality check passed: jobId={}, qualityScore={}, status=PASS",
                        job.getId(), report.getQualityScore());
            } else if (report.getOverallStatus() == OverallStatus.WARN) {
                List<String> failedRules = report.getRuleResults() != null
                        ? report.getRuleResults().stream()
                            .filter(r -> !r.isPassed())
                            .map(ValidationRuleResult::getRuleName)
                            .collect(Collectors.toList())
                        : Collections.emptyList();
                log.warn("Quality check warnings: jobId={}, qualityScore={}, failedRules={}",
                        job.getId(), report.getQualityScore(), failedRules);
            }

            return report;
        } catch (Exception e) {
            log.warn("Quality check unavailable for jobId={}, proceeding without gate: {}",
                    job.getId(), e.getMessage());
            return null;
        }
    }
}
