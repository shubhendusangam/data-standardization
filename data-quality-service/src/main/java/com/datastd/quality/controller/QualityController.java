package com.datastd.quality.controller;

import com.datastd.common.dto.OverallStatus;
import com.datastd.common.dto.QualityReport;
import com.datastd.quality.dto.*;
import com.datastd.quality.entity.ValidationType;
import com.datastd.quality.service.QualityService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/quality")
public class QualityController {

    private final QualityService qualityService;

    public QualityController(QualityService qualityService) {
        this.qualityService = qualityService;
    }

    // === Validation Rules ===

    @PostMapping("/rules")
    public ResponseEntity<ValidationRuleResponse> createRule(@Valid @RequestBody ValidationRuleRequest request) {
        ValidationRuleResponse response = qualityService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/rules")
    public ResponseEntity<List<ValidationRuleResponse>> getAllRules(
            @RequestParam(required = false) String columnName,
            @RequestParam(required = false) ValidationType validationType,
            @RequestParam(required = false) Boolean active) {
        List<ValidationRuleResponse> rules = qualityService.getAllRules(columnName, validationType, active);
        return ResponseEntity.ok(rules);
    }

    @PutMapping("/rules/{id}")
    public ResponseEntity<ValidationRuleResponse> updateRule(@PathVariable UUID id,
                                                              @Valid @RequestBody ValidationRuleRequest request) {
        ValidationRuleResponse response = qualityService.updateRule(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/rules/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        qualityService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/rules/{id}/toggle")
    public ResponseEntity<ValidationRuleResponse> toggleRule(@PathVariable UUID id) {
        ValidationRuleResponse response = qualityService.toggleRule(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/rules/templates")
    public ResponseEntity<List<ValidationRuleResponse>> getTemplateRules() {
        List<ValidationRuleResponse> templates = qualityService.getTemplateRules();
        return ResponseEntity.ok(templates);
    }

    @PostMapping("/rules/suggest")
    public ResponseEntity<List<SuggestedRule>> suggestRules(@RequestBody SuggestRequest request) {
        List<SuggestedRule> suggestions = qualityService.suggestRules(request.getDatasetId());
        return ResponseEntity.ok(suggestions);
    }

    // === Validation Rule Sets ===

    @PostMapping("/rulesets")
    public ResponseEntity<ValidationRuleSetResponse> createRuleSet(@Valid @RequestBody ValidationRuleSetRequest request) {
        ValidationRuleSetResponse response = qualityService.createRuleSet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/rulesets")
    public ResponseEntity<List<ValidationRuleSetResponse>> getAllRuleSets() {
        List<ValidationRuleSetResponse> ruleSets = qualityService.getAllRuleSets();
        return ResponseEntity.ok(ruleSets);
    }

    @GetMapping("/rulesets/{id}")
    public ResponseEntity<ValidationRuleSetResponse> getRuleSetById(@PathVariable UUID id) {
        ValidationRuleSetResponse response = qualityService.getRuleSetById(id);
        return ResponseEntity.ok(response);
    }

    // === Validation Execution ===

    @PostMapping("/validate")
    public ResponseEntity<QualityReport> runValidation(@RequestBody ValidateRequest request) {
        QualityReport report = qualityService.runValidation(request);
        return ResponseEntity.ok(report);
    }

    // === Reports ===

    @GetMapping("/reports/{datasetId}")
    public ResponseEntity<QualityReport> getLatestReport(@PathVariable UUID datasetId) {
        QualityReport report = qualityService.getLatestReport(datasetId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/reports/{reportId}/full")
    public ResponseEntity<QualityReport> getFullReport(@PathVariable UUID reportId) {
        QualityReport report = qualityService.getFullReport(reportId);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/reports")
    public ResponseEntity<List<QualityReport>> listReports(
            @RequestParam(required = false) UUID datasetId,
            @RequestParam(required = false) OverallStatus overallStatus) {
        List<QualityReport> reports = qualityService.listReports(datasetId, overallStatus);
        return ResponseEntity.ok(reports);
    }

    @GetMapping("/reports/history")
    public ResponseEntity<Page<QualityReportSummary>> getReportHistory(
            @RequestParam UUID datasetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "evaluatedAt"));
        Page<QualityReportSummary> history = qualityService.getReportHistory(datasetId, pageable);
        return ResponseEntity.ok(history);
    }

    // === Trend ===

    @GetMapping("/datasets/{datasetId}/trend")
    public ResponseEntity<QualityTrendResponse> getDatasetTrend(
            @PathVariable UUID datasetId,
            @RequestParam(defaultValue = "30") int days) {
        QualityTrendResponse trend = qualityService.getDatasetTrend(datasetId, days);
        return ResponseEntity.ok(trend);
    }

    // === Summary ===

    @GetMapping("/summary")
    public ResponseEntity<List<DatasetQualitySummary>> getDatasetSummaries(
            @RequestParam(required = false) String status) {
        List<DatasetQualitySummary> summaries = qualityService.getDatasetSummaries(status);
        return ResponseEntity.ok(summaries);
    }

    // === Alerts ===

    @PostMapping("/alerts")
    public ResponseEntity<AlertConfigResponse> createAlertConfig(@RequestBody AlertConfigRequest request) {
        AlertConfigResponse response = qualityService.createAlertConfig(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/alerts")
    public ResponseEntity<List<AlertConfigResponse>> getAllAlertConfigs() {
        List<AlertConfigResponse> configs = qualityService.getAllAlertConfigs();
        return ResponseEntity.ok(configs);
    }

    @PutMapping("/alerts/{id}")
    public ResponseEntity<AlertConfigResponse> updateAlertConfig(@PathVariable UUID id,
                                                                  @RequestBody AlertConfigRequest request) {
        AlertConfigResponse response = qualityService.updateAlertConfig(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/alerts/{id}")
    public ResponseEntity<Void> deleteAlertConfig(@PathVariable UUID id) {
        qualityService.deleteAlertConfig(id);
        return ResponseEntity.noContent().build();
    }
}
