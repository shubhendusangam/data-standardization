package com.datastd.quality.service;

import com.datastd.common.dto.OverallStatus;
import com.datastd.common.dto.QualityReport;
import com.datastd.quality.dto.*;
import com.datastd.quality.entity.ValidationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface QualityService {

    // === Validation Rules ===
    ValidationRuleResponse createRule(ValidationRuleRequest request);
    List<ValidationRuleResponse> getAllRules(String columnName, ValidationType validationType, Boolean active);
    ValidationRuleResponse updateRule(UUID id, ValidationRuleRequest request);
    void deleteRule(UUID id);
    ValidationRuleResponse toggleRule(UUID id);
    List<ValidationRuleResponse> getTemplateRules();

    // === Rule Suggestions ===
    List<SuggestedRule> suggestRules(UUID datasetId);

    // === Validation Rule Sets ===
    ValidationRuleSetResponse createRuleSet(ValidationRuleSetRequest request);
    List<ValidationRuleSetResponse> getAllRuleSets();
    ValidationRuleSetResponse getRuleSetById(UUID id);

    // === Validation Execution ===
    QualityReport runValidation(ValidateRequest request);

    // === Reports ===
    QualityReport getLatestReport(UUID datasetId);
    List<QualityReport> listReports(UUID datasetId, OverallStatus overallStatus);
    QualityReport getFullReport(UUID reportId);
    Page<QualityReportSummary> getReportHistory(UUID datasetId, Pageable pageable);

    // === Trend ===
    QualityTrendResponse getDatasetTrend(UUID datasetId, int days);

    // === Summary ===
    List<DatasetQualitySummary> getDatasetSummaries(String statusFilter);

    // === Alerts ===
    AlertConfigResponse createAlertConfig(AlertConfigRequest request);
    List<AlertConfigResponse> getAllAlertConfigs();
    AlertConfigResponse updateAlertConfig(UUID id, AlertConfigRequest request);
    void deleteAlertConfig(UUID id);
}
