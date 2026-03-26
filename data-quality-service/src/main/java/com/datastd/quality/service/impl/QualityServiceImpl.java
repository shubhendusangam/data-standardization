package com.datastd.quality.service.impl;

import com.datastd.common.dto.ColumnReport;
import com.datastd.common.dto.IngestedDatasetResponse;
import com.datastd.common.dto.OverallStatus;
import com.datastd.common.dto.QualityReport;
import com.datastd.quality.client.IngestionServiceClient;
import com.datastd.quality.dto.*;
import com.datastd.quality.engine.ColumnProfiler;
import com.datastd.quality.engine.RuleSuggestionEngine;
import com.datastd.quality.engine.TrendEngine;
import com.datastd.quality.engine.ValidationEngine;
import com.datastd.quality.entity.QualityAlertConfig;
import com.datastd.quality.entity.QualityReportEntity;
import com.datastd.quality.entity.ValidationRule;
import com.datastd.quality.entity.ValidationRuleSet;
import com.datastd.quality.entity.ValidationType;
import com.datastd.quality.exception.DataProcessingException;
import com.datastd.quality.exception.ResourceNotFoundException;
import com.datastd.quality.mapper.QualityMapper;
import com.datastd.quality.repository.QualityAlertConfigRepository;
import com.datastd.quality.repository.QualityReportRepository;
import com.datastd.quality.repository.ValidationRuleRepository;
import com.datastd.quality.repository.ValidationRuleSetRepository;
import com.datastd.quality.service.QualityAlertService;
import com.datastd.quality.service.QualityService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class QualityServiceImpl implements QualityService {

    private static final Logger log = LoggerFactory.getLogger(QualityServiceImpl.class);

    private final ValidationRuleRepository ruleRepository;
    private final ValidationRuleSetRepository ruleSetRepository;
    private final QualityReportRepository reportRepository;
    private final QualityAlertConfigRepository alertConfigRepository;
    private final IngestionServiceClient ingestionClient;
    private final ValidationEngine validationEngine;
    private final ColumnProfiler columnProfiler;
    private final RuleSuggestionEngine ruleSuggestionEngine;
    private final TrendEngine trendEngine;
    private final QualityAlertService qualityAlertService;
    private final ObjectMapper objectMapper;

    public QualityServiceImpl(ValidationRuleRepository ruleRepository,
                              ValidationRuleSetRepository ruleSetRepository,
                              QualityReportRepository reportRepository,
                              QualityAlertConfigRepository alertConfigRepository,
                              IngestionServiceClient ingestionClient,
                              ValidationEngine validationEngine,
                              ColumnProfiler columnProfiler,
                              RuleSuggestionEngine ruleSuggestionEngine,
                              TrendEngine trendEngine,
                              QualityAlertService qualityAlertService,
                              ObjectMapper objectMapper) {
        this.ruleRepository = ruleRepository;
        this.ruleSetRepository = ruleSetRepository;
        this.reportRepository = reportRepository;
        this.alertConfigRepository = alertConfigRepository;
        this.ingestionClient = ingestionClient;
        this.validationEngine = validationEngine;
        this.columnProfiler = columnProfiler;
        this.ruleSuggestionEngine = ruleSuggestionEngine;
        this.trendEngine = trendEngine;
        this.qualityAlertService = qualityAlertService;
        this.objectMapper = objectMapper;
    }

    // ── Validation Rules ────────────────────────────────────────────

    @Override
    public ValidationRuleResponse createRule(ValidationRuleRequest request) {
        log.info("Creating validation rule: name={}, column={}, type={}",
                request.getName(), request.getColumnName(), request.getValidationType());
        ValidationRule rule = new ValidationRule();
        rule.setName(request.getName());
        rule.setColumnName(request.getColumnName());
        rule.setValidationType(request.getValidationType());
        rule.setParams(request.getParams());
        rule.setSeverity(request.getSeverity());
        rule.setActive(request.isActive());

        ValidationRule saved = ruleRepository.save(rule);
        log.info("Validation rule created: id={}", saved.getId());
        return QualityMapper.toResponse(saved);
    }

    @Override
    public List<ValidationRuleResponse> getAllRules(String columnName, ValidationType validationType, Boolean active) {
        log.debug("Listing validation rules: columnName={}, type={}, active={}", columnName, validationType, active);
        List<ValidationRule> rules;

        if (columnName != null) {
            rules = ruleRepository.findByColumnName(columnName);
        } else if (validationType != null) {
            rules = ruleRepository.findByValidationType(validationType);
        } else if (active != null) {
            rules = ruleRepository.findByActive(active);
        } else {
            rules = ruleRepository.findAll();
        }

        return rules.stream().map(QualityMapper::toResponse).collect(Collectors.toList());
    }

    @Override
    public ValidationRuleResponse updateRule(UUID id, ValidationRuleRequest request) {
        log.info("Updating validation rule: id={}", id);
        ValidationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Validation rule not found: " + id));
        rule.setName(request.getName());
        rule.setColumnName(request.getColumnName());
        rule.setValidationType(request.getValidationType());
        rule.setParams(request.getParams());
        rule.setSeverity(request.getSeverity());
        rule.setActive(request.isActive());

        ValidationRule saved = ruleRepository.save(rule);
        log.info("Validation rule updated: id={}", saved.getId());
        return QualityMapper.toResponse(saved);
    }

    @Override
    public void deleteRule(UUID id) {
        if (!ruleRepository.existsById(id)) {
            throw new ResourceNotFoundException("Validation rule not found: " + id);
        }
        ruleRepository.deleteById(id);
        log.info("Validation rule deleted: id={}", id);
    }

    @Override
    public ValidationRuleResponse toggleRule(UUID id) {
        ValidationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Validation rule not found: " + id));
        rule.setActive(!rule.isActive());
        ValidationRule saved = ruleRepository.save(rule);
        log.info("Validation rule toggled: id={}, active={}", saved.getId(), saved.isActive());
        return QualityMapper.toResponse(saved);
    }

    @Override
    public List<ValidationRuleResponse> getTemplateRules() {
        log.debug("Listing template validation rules");
        return ruleRepository.findAll().stream()
                .map(QualityMapper::toResponse)
                .collect(Collectors.toList());
    }

    // ── Rule Suggestions ─────────────────────────────────────────────

    @Override
    public List<SuggestedRule> suggestRules(UUID datasetId) {
        log.info("Generating rule suggestions for datasetId={}", datasetId);
        IngestedDatasetResponse dataset = ingestionClient.getDatasetById(datasetId);
        List<Map<String, Object>> records = parseRawData(dataset.getRawData());
        List<ColumnReport> columnProfiles = columnProfiler.profileDataset(records);
        List<SuggestedRule> suggestions = ruleSuggestionEngine.suggest(columnProfiles);
        log.info("Generated {} suggestions for datasetId={}", suggestions.size(), datasetId);
        return suggestions;
    }

    // ── Validation Rule Sets ────────────────────────────────────────

    @Override
    public ValidationRuleSetResponse createRuleSet(ValidationRuleSetRequest request) {
        log.info("Creating validation rule set: name={}, ruleCount={}", request.getName(), request.getRuleIds().size());
        List<ValidationRule> rules = ruleRepository.findByIdIn(request.getRuleIds());
        if (rules.size() != request.getRuleIds().size()) {
            throw new IllegalArgumentException("Some validation rule IDs are invalid");
        }

        ValidationRuleSet ruleSet = new ValidationRuleSet();
        ruleSet.setName(request.getName());
        ruleSet.setDescription(request.getDescription());
        ruleSet.setRuleIds(request.getRuleIds());

        ValidationRuleSet saved = ruleSetRepository.save(ruleSet);
        List<ValidationRuleResponse> ruleResponses = rules.stream()
                .map(QualityMapper::toResponse).collect(Collectors.toList());
        log.info("Validation rule set created: id={}", saved.getId());
        return QualityMapper.toResponse(saved, ruleResponses);
    }

    @Override
    public List<ValidationRuleSetResponse> getAllRuleSets() {
        log.debug("Listing all validation rule sets");
        return ruleSetRepository.findAll().stream()
                .map(QualityMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ValidationRuleSetResponse getRuleSetById(UUID id) {
        log.debug("Fetching validation rule set: id={}", id);
        ValidationRuleSet ruleSet = ruleSetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Validation rule set not found: " + id));

        List<ValidationRule> rules = ruleRepository.findByIdIn(ruleSet.getRuleIds());
        List<ValidationRuleResponse> ruleResponses = rules.stream()
                .map(QualityMapper::toResponse).collect(Collectors.toList());
        return QualityMapper.toResponse(ruleSet, ruleResponses);
    }

    // ── Validation Execution ────────────────────────────────────────

    @Override
    public QualityReport runValidation(ValidateRequest request) {
        UUID datasetId = request.getDatasetId();
        log.info("Running validation for datasetId={}", datasetId);

        IngestedDatasetResponse dataset = ingestionClient.getDatasetById(datasetId);
        List<Map<String, Object>> records = parseRawData(dataset.getRawData());
        List<ValidationRule> rules = resolveRules(request);
        QualityReport report = validationEngine.validate(datasetId, records, rules);
        persistReport(report);

        // Fire-and-forget alert evaluation (async)
        qualityAlertService.evaluateAndFireAlerts(report);

        return report;
    }

    private List<ValidationRule> resolveRules(ValidateRequest request) {
        if (request.getRuleSetId() != null) {
            ValidationRuleSet ruleSet = ruleSetRepository.findById(request.getRuleSetId())
                    .orElseThrow(() -> new ResourceNotFoundException("Validation rule set not found: " + request.getRuleSetId()));
            return ruleRepository.findByIdIn(ruleSet.getRuleIds());
        } else if (request.getRuleIds() != null && !request.getRuleIds().isEmpty()) {
            return ruleRepository.findByIdIn(request.getRuleIds());
        } else {
            return ruleRepository.findByActive(true);
        }
    }

    private List<Map<String, Object>> parseRawData(String rawData) {
        if (rawData == null || rawData.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawData, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse rawData as JSON array: {}", e.getMessage());
            throw new DataProcessingException("Failed to parse dataset raw data as JSON array", e);
        }
    }

    private void persistReport(QualityReport report) {
        try {
            String json = objectMapper.writeValueAsString(report);
            QualityReportEntity entity = new QualityReportEntity();
            entity.setDatasetId(report.getDatasetId());
            entity.setOverallStatus(report.getOverallStatus().name());
            entity.setQualityScore(report.getQualityScore());
            entity.setTotalRecords(report.getTotalRecords());
            entity.setDuplicateCount(report.getDuplicateCount());
            entity.setReportJson(json);
            entity.setEvaluatedAt(report.getEvaluatedAt());
            QualityReportEntity saved = reportRepository.save(entity);
            report.setReportId(saved.getId());
            log.info("Quality report persisted: datasetId={}, reportId={}, status={}",
                    report.getDatasetId(), saved.getId(), report.getOverallStatus());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize QualityReport to JSON: {}", e.getMessage());
            throw new DataProcessingException("Failed to serialize quality report", e);
        }
    }

    // ── Reports ─────────────────────────────────────────────────────

    @Override
    public QualityReport getLatestReport(UUID datasetId) {
        log.debug("Fetching latest report for datasetId={}", datasetId);
        QualityReportEntity entity = reportRepository.findTopByDatasetIdOrderByEvaluatedAtDesc(datasetId)
                .orElseThrow(() -> new ResourceNotFoundException("No quality report found for dataset: " + datasetId));
        return deserializeReport(entity);
    }

    @Override
    public List<QualityReport> listReports(UUID datasetId, OverallStatus overallStatus) {
        log.debug("Listing reports: datasetId={}, overallStatus={}", datasetId, overallStatus);
        List<QualityReportEntity> entities;

        if (datasetId != null) {
            entities = reportRepository.findByDatasetIdOrderByEvaluatedAtDesc(datasetId);
        } else if (overallStatus != null) {
            entities = reportRepository.findByOverallStatusOrderByEvaluatedAtDesc(overallStatus.name());
        } else {
            entities = reportRepository.findAllByOrderByEvaluatedAtDesc();
        }

        return entities.stream().map(this::deserializeReport).collect(Collectors.toList());
    }

    @Override
    public QualityReport getFullReport(UUID reportId) {
        log.debug("Fetching full report: reportId={}", reportId);
        QualityReportEntity entity = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Quality report not found: " + reportId));
        return deserializeReport(entity);
    }

    @Override
    public Page<QualityReportSummary> getReportHistory(UUID datasetId, Pageable pageable) {
        log.debug("Fetching report history: datasetId={}, page={}", datasetId, pageable);
        Page<QualityReportEntity> page = reportRepository.findByDatasetIdOrderByEvaluatedAtDesc(datasetId, pageable);
        return page.map(this::toSummary);
    }

    // ── Trend ───────────────────────────────────────────────────────

    @Override
    public QualityTrendResponse getDatasetTrend(UUID datasetId, int days) {
        log.debug("Computing trend: datasetId={}, days={}", datasetId, days);
        Instant after = Instant.now().minus(days, ChronoUnit.DAYS);
        List<QualityReportEntity> reports =
                reportRepository.findByDatasetIdAndEvaluatedAtAfterOrderByEvaluatedAtAsc(datasetId, after);

        QualityTrendResponse response = new QualityTrendResponse();
        response.setDatasetId(datasetId);

        List<QualityTrendResponse.TrendPoint> trendPoints = reports.stream()
                .map(e -> new QualityTrendResponse.TrendPoint(
                        e.getEvaluatedAt(), e.getQualityScore(), e.getOverallStatus(), e.getTotalRecords()))
                .collect(Collectors.toList());
        response.setTrendPoints(trendPoints);

        if (!reports.isEmpty()) {
            double avg = reports.stream().mapToInt(QualityReportEntity::getQualityScore).average().orElse(0);
            double min = reports.stream().mapToInt(QualityReportEntity::getQualityScore).min().orElse(0);
            response.setAvgScore(Math.round(avg * 10.0) / 10.0);
            response.setMinScore(min);
        }

        response.setTrend(trendEngine.computeTrend(reports));
        return response;
    }

    // ── Summary ─────────────────────────────────────────────────────

    @Override
    public List<DatasetQualitySummary> getDatasetSummaries(String statusFilter) {
        log.debug("Computing dataset summaries, statusFilter={}", statusFilter);
        List<UUID> datasetIds = reportRepository.findDistinctDatasetIds();

        List<DatasetQualitySummary> summaries = new ArrayList<>();
        for (UUID dsId : datasetIds) {
            Optional<QualityReportEntity> latest = reportRepository.findTopByDatasetIdOrderByEvaluatedAtDesc(dsId);
            if (latest.isEmpty()) continue;

            QualityReportEntity e = latest.get();

            if (statusFilter != null && !statusFilter.isBlank()
                    && !e.getOverallStatus().equalsIgnoreCase(statusFilter)) {
                continue;
            }

            DatasetQualitySummary summary = new DatasetQualitySummary();
            summary.setDatasetId(dsId);
            summary.setLatestScore(e.getQualityScore());
            summary.setLatestStatus(e.getOverallStatus());
            summary.setLastEvaluatedAt(e.getEvaluatedAt());

            // Compute trend from last 30 days
            Instant after = Instant.now().minus(30, ChronoUnit.DAYS);
            List<QualityReportEntity> recentReports =
                    reportRepository.findByDatasetIdAndEvaluatedAtAfterOrderByEvaluatedAtAsc(dsId, after);
            summary.setTrend(trendEngine.computeTrend(recentReports));

            summaries.add(summary);
        }

        // Sort by latestScore ASC (worst first)
        summaries.sort(Comparator.comparingInt(DatasetQualitySummary::getLatestScore));
        return summaries;
    }

    // ── Alerts CRUD ─────────────────────────────────────────────────

    @Override
    public AlertConfigResponse createAlertConfig(AlertConfigRequest request) {
        log.info("Creating alert config: name={}, url={}", request.getName(), request.getWebhookUrl());
        QualityAlertConfig config = new QualityAlertConfig();
        config.setName(request.getName());
        config.setWebhookUrl(request.getWebhookUrl());
        config.setTriggerOnStatus(request.getTriggerOnStatus() != null
                ? String.join(",", request.getTriggerOnStatus()) : "");
        config.setTriggerOnScoreBelow(request.getTriggerOnScoreBelow());
        config.setActive(request.isActive());
        config.setSecret(request.getSecret() != null ? request.getSecret() : "");

        QualityAlertConfig saved = alertConfigRepository.save(config);
        log.info("Alert config created: id={}", saved.getId());
        return toAlertResponse(saved);
    }

    @Override
    public List<AlertConfigResponse> getAllAlertConfigs() {
        return alertConfigRepository.findAll().stream()
                .map(this::toAlertResponse)
                .collect(Collectors.toList());
    }

    @Override
    public AlertConfigResponse updateAlertConfig(UUID id, AlertConfigRequest request) {
        QualityAlertConfig config = alertConfigRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alert config not found: " + id));
        config.setName(request.getName());
        config.setWebhookUrl(request.getWebhookUrl());
        config.setTriggerOnStatus(request.getTriggerOnStatus() != null
                ? String.join(",", request.getTriggerOnStatus()) : "");
        config.setTriggerOnScoreBelow(request.getTriggerOnScoreBelow());
        config.setActive(request.isActive());
        if (request.getSecret() != null) {
            config.setSecret(request.getSecret());
        }
        QualityAlertConfig saved = alertConfigRepository.save(config);
        return toAlertResponse(saved);
    }

    @Override
    public void deleteAlertConfig(UUID id) {
        if (!alertConfigRepository.existsById(id)) {
            throw new ResourceNotFoundException("Alert config not found: " + id);
        }
        alertConfigRepository.deleteById(id);
        log.info("Alert config deleted: id={}", id);
    }

    // ── Private Helpers ─────────────────────────────────────────────

    private QualityReport deserializeReport(QualityReportEntity entity) {
        try {
            QualityReport report = objectMapper.readValue(entity.getReportJson(), QualityReport.class);
            report.setReportId(entity.getId());
            return report;
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize report JSON: {}", e.getMessage());
            throw new DataProcessingException("Failed to deserialize quality report", e);
        }
    }

    private QualityReportSummary toSummary(QualityReportEntity entity) {
        return new QualityReportSummary(
                entity.getId(), entity.getDatasetId(), entity.getOverallStatus(),
                entity.getQualityScore(), entity.getEvaluatedAt(),
                entity.getTotalRecords(), entity.getDuplicateCount());
    }

    private AlertConfigResponse toAlertResponse(QualityAlertConfig config) {
        AlertConfigResponse response = new AlertConfigResponse();
        response.setId(config.getId());
        response.setName(config.getName());
        response.setWebhookUrl(config.getWebhookUrl());
        String statusStr = config.getTriggerOnStatus();
        response.setTriggerOnStatus(statusStr != null && !statusStr.isEmpty()
                ? List.of(statusStr.split(",")) : List.of());
        response.setTriggerOnScoreBelow(config.getTriggerOnScoreBelow());
        response.setActive(config.isActive());
        response.setCreatedAt(config.getCreatedAt());
        return response;
    }
}
