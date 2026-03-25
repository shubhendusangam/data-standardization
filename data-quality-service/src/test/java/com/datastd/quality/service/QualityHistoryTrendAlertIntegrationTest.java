package com.datastd.quality.service;

import com.datastd.common.dto.OverallStatus;
import com.datastd.common.dto.QualityReport;
import com.datastd.common.dto.ValidationRuleResult;
import com.datastd.quality.dto.*;
import com.datastd.quality.engine.TrendEngine;
import com.datastd.quality.entity.QualityAlertConfig;
import com.datastd.quality.entity.QualityReportEntity;
import com.datastd.quality.repository.QualityAlertConfigRepository;
import com.datastd.quality.repository.QualityReportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for quality history, trend scoring, alerts, summary, and retention.
 */
@SpringBootTest
@ActiveProfiles("test")
class QualityHistoryTrendAlertIntegrationTest {

    @Autowired
    private QualityService qualityService;

    @Autowired
    private QualityReportRepository reportRepository;

    @Autowired
    private QualityAlertConfigRepository alertConfigRepository;

    @Autowired
    private TrendEngine trendEngine;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID datasetIdA = UUID.randomUUID();
    private final UUID datasetIdB = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        alertConfigRepository.deleteAll();
        reportRepository.deleteAll();
    }

    // ── Helper: seed report entities directly ───────────────────────

    private QualityReportEntity seedReport(UUID datasetId, int score, String status, Instant evaluatedAt) {
        try {
            QualityReport report = new QualityReport();
            report.setDatasetId(datasetId);
            report.setOverallStatus(OverallStatus.valueOf(status));
            report.setQualityScore(score);
            report.setTotalRecords(100);
            report.setDuplicateCount(0);
            report.setColumnReports(List.of());
            report.setRuleResults(List.of());
            report.setEvaluatedAt(evaluatedAt);

            QualityReportEntity entity = new QualityReportEntity();
            entity.setDatasetId(datasetId);
            entity.setOverallStatus(status);
            entity.setQualityScore(score);
            entity.setTotalRecords(100);
            entity.setDuplicateCount(0);
            entity.setReportJson(objectMapper.writeValueAsString(report));
            entity.setEvaluatedAt(evaluatedAt);
            return reportRepository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── 1. Report History ───────────────────────────────────────────

    @Test
    void reportHistory_returnsPaginatedSummaries() {
        Instant base = Instant.now().minus(10, ChronoUnit.DAYS);
        for (int i = 0; i < 5; i++) {
            seedReport(datasetIdA, 80 + i, "PASS", base.plus(i, ChronoUnit.DAYS));
        }

        Page<QualityReportSummary> page = qualityService.getReportHistory(datasetIdA, PageRequest.of(0, 3));
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getContent()).hasSize(3);
        // Summaries should NOT include columnReports/ruleResults (they're in QualityReportSummary, not QualityReport)
        assertThat(page.getContent().get(0).getReportId()).isNotNull();
        assertThat(page.getContent().get(0).getQualityScore()).isGreaterThan(0);
    }

    @Test
    void fullReport_returnsCompleteData() {
        QualityReportEntity entity = seedReport(datasetIdA, 75, "WARN", Instant.now());
        QualityReport full = qualityService.getFullReport(entity.getId());
        assertThat(full.getReportId()).isEqualTo(entity.getId());
        assertThat(full.getQualityScore()).isEqualTo(75);
        assertThat(full.getOverallStatus()).isEqualTo(OverallStatus.WARN);
    }

    // ── 2. Quality Trend ────────────────────────────────────────────

    @Test
    void trend_degrading_whenScoresDecrease() {
        Instant base = Instant.now().minus(10, ChronoUnit.DAYS);
        // Decreasing scores: 90, 80, 70, 60, 50
        for (int i = 0; i < 5; i++) {
            seedReport(datasetIdA, 90 - i * 10, "FAIL", base.plus(i, ChronoUnit.DAYS));
        }

        QualityTrendResponse trend = qualityService.getDatasetTrend(datasetIdA, 30);
        assertThat(trend.getTrend()).isEqualTo("DEGRADING");
        assertThat(trend.getTrendPoints()).hasSize(5);
        assertThat(trend.getMinScore()).isEqualTo(50);
    }

    @Test
    void trend_improving_whenScoresIncrease() {
        Instant base = Instant.now().minus(10, ChronoUnit.DAYS);
        // Increasing scores: 50, 60, 70, 80, 90
        for (int i = 0; i < 5; i++) {
            seedReport(datasetIdA, 50 + i * 10, "PASS", base.plus(i, ChronoUnit.DAYS));
        }

        QualityTrendResponse trend = qualityService.getDatasetTrend(datasetIdA, 30);
        assertThat(trend.getTrend()).isEqualTo("IMPROVING");
    }

    @Test
    void trend_stable_whenScoresFlat() {
        Instant base = Instant.now().minus(10, ChronoUnit.DAYS);
        for (int i = 0; i < 5; i++) {
            seedReport(datasetIdA, 80, "PASS", base.plus(i, ChronoUnit.DAYS));
        }

        QualityTrendResponse trend = qualityService.getDatasetTrend(datasetIdA, 30);
        assertThat(trend.getTrend()).isEqualTo("STABLE");
    }

    @Test
    void trend_unknown_whenLessThan2DataPoints() {
        seedReport(datasetIdA, 80, "PASS", Instant.now());

        QualityTrendResponse trend = qualityService.getDatasetTrend(datasetIdA, 30);
        assertThat(trend.getTrend()).isEqualTo("UNKNOWN");
    }

    // ── 3. Alert Webhook CRUD ───────────────────────────────────────

    @Test
    void alertCrud_createListUpdateDelete() {
        AlertConfigRequest request = new AlertConfigRequest();
        request.setName("test-alert");
        request.setWebhookUrl("http://localhost:9999/webhook");
        request.setTriggerOnStatus(List.of("FAIL", "WARN"));
        request.setTriggerOnScoreBelow(50);
        request.setActive(true);
        request.setSecret("my-secret");

        // Create
        AlertConfigResponse created = qualityService.createAlertConfig(request);
        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("test-alert");
        assertThat(created.getTriggerOnStatus()).containsExactly("FAIL", "WARN");

        // List
        List<AlertConfigResponse> all = qualityService.getAllAlertConfigs();
        assertThat(all).hasSize(1);

        // Update
        request.setName("updated-alert");
        request.setTriggerOnScoreBelow(60);
        AlertConfigResponse updated = qualityService.updateAlertConfig(created.getId(), request);
        assertThat(updated.getName()).isEqualTo("updated-alert");
        assertThat(updated.getTriggerOnScoreBelow()).isEqualTo(60);

        // Delete
        qualityService.deleteAlertConfig(created.getId());
        assertThat(qualityService.getAllAlertConfigs()).isEmpty();
    }

    @Test
    void hmacSignature_isVerifiable() {
        String data = "{\"event\":\"QUALITY_ALERT\",\"qualityScore\":42}";
        String secret = "test-secret-key";

        String signature = QualityAlertService.computeHmac(data, secret);
        assertThat(signature).isNotBlank();
        assertThat(signature).hasSize(64); // SHA-256 hex = 64 chars

        // Same input produces same signature (deterministic)
        String signature2 = QualityAlertService.computeHmac(data, secret);
        assertThat(signature).isEqualTo(signature2);

        // Different secret produces different signature
        String signature3 = QualityAlertService.computeHmac(data, "different-secret");
        assertThat(signature).isNotEqualTo(signature3);
    }

    // ── 4. Dataset Summary ──────────────────────────────────────────

    @Test
    void summary_returnsWorstFirst() {
        seedReport(datasetIdA, 90, "PASS", Instant.now());
        seedReport(datasetIdB, 40, "FAIL", Instant.now());

        List<DatasetQualitySummary> summaries = qualityService.getDatasetSummaries(null);
        assertThat(summaries).hasSize(2);
        // Worst first
        assertThat(summaries.get(0).getLatestScore()).isEqualTo(40);
        assertThat(summaries.get(1).getLatestScore()).isEqualTo(90);
    }

    @Test
    void summary_filtersbyStatus() {
        seedReport(datasetIdA, 90, "PASS", Instant.now());
        seedReport(datasetIdB, 40, "FAIL", Instant.now());

        List<DatasetQualitySummary> failOnly = qualityService.getDatasetSummaries("FAIL");
        assertThat(failOnly).hasSize(1);
        assertThat(failOnly.get(0).getLatestStatus()).isEqualTo("FAIL");
    }

    @Test
    void summary_includesTrend() {
        Instant base = Instant.now().minus(10, ChronoUnit.DAYS);
        for (int i = 0; i < 5; i++) {
            seedReport(datasetIdA, 90 - i * 10, "FAIL", base.plus(i, ChronoUnit.DAYS));
        }

        List<DatasetQualitySummary> summaries = qualityService.getDatasetSummaries(null);
        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).getTrend()).isEqualTo("DEGRADING");
    }

    // ── 5. Report Retention ─────────────────────────────────────────

    @Test
    @Transactional
    void retention_deletesOldReports_preservesRecent() {
        // Seed old report (100 days ago)
        seedReport(datasetIdA, 80, "PASS", Instant.now().minus(100, ChronoUnit.DAYS));
        // Seed recent report (1 day ago)
        seedReport(datasetIdA, 90, "PASS", Instant.now().minus(1, ChronoUnit.DAYS));

        assertThat(reportRepository.findAll()).hasSize(2);

        // Manually trigger cleanup (retention = 90 days in prod, but we use cutoff directly)
        Instant cutoff = Instant.now().minus(90, ChronoUnit.DAYS);
        int deleted = reportRepository.deleteByEvaluatedAtBefore(cutoff);
        assertThat(deleted).isEqualTo(1);
        assertThat(reportRepository.findAll()).hasSize(1);
        assertThat(reportRepository.findAll().get(0).getQualityScore()).isEqualTo(90);
    }

    // ── TrendEngine unit-level tests ────────────────────────────────

    @Test
    void trendEngine_directTest_degrading() {
        List<QualityReportEntity> reports = new ArrayList<>();
        Instant base = Instant.now().minus(10, ChronoUnit.DAYS);
        for (int i = 0; i < 5; i++) {
            QualityReportEntity e = new QualityReportEntity();
            e.setQualityScore(90 - i * 10);
            e.setEvaluatedAt(base.plus(i, ChronoUnit.DAYS));
            reports.add(e);
        }
        assertThat(trendEngine.computeTrend(reports)).isEqualTo("DEGRADING");
    }

    @Test
    void trendEngine_directTest_improving() {
        List<QualityReportEntity> reports = new ArrayList<>();
        Instant base = Instant.now().minus(10, ChronoUnit.DAYS);
        for (int i = 0; i < 5; i++) {
            QualityReportEntity e = new QualityReportEntity();
            e.setQualityScore(50 + i * 10);
            e.setEvaluatedAt(base.plus(i, ChronoUnit.DAYS));
            reports.add(e);
        }
        assertThat(trendEngine.computeTrend(reports)).isEqualTo("IMPROVING");
    }
}

