package com.datastd.standardization.service;

import com.datastd.common.dto.*;
import com.datastd.standardization.client.DataQualityClient;
import com.datastd.standardization.client.IngestionServiceClient;
import com.datastd.standardization.client.RuleEngineClient;
import com.datastd.standardization.dto.ProcessingRequest;
import com.datastd.standardization.dto.ProcessingStatusResponse;
import com.datastd.standardization.dto.QualityValidateRequest;
import com.datastd.standardization.dto.StandardizedResultResponse;
import com.datastd.standardization.repository.ProcessingJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the quality gate feature in standardization-service.
 * Feign clients are mocked; everything else (JPA, async, service logic) runs for real.
 */
@SpringBootTest
@ActiveProfiles("test")
class QualityGateIntegrationTest {

    @MockBean
    private IngestionServiceClient ingestionClient;

    @MockBean
    private RuleEngineClient ruleEngineClient;

    @MockBean
    private DataQualityClient dataQualityClient;

    @Autowired
    private ProcessingJobRepository jobRepository;

    @Autowired
    private com.datastd.standardization.service.StandardizationService standardizationService;

    @Autowired
    private ObjectMapper objectMapper;

    private final UUID datasetId = UUID.randomUUID();
    private final UUID ruleId = UUID.randomUUID();
    private final UUID qualityRuleSetId = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        jobRepository.deleteAll();

        // Stub ingestion-service: dataset with 10 records (1 has null email → 10% null rate)
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Map<String, Object> record = new LinkedHashMap<>();
            record.put("name", "User" + i);
            record.put("email", i == 0 ? null : "user" + i + "@test.com");
            records.add(record);
        }
        IngestedDatasetResponse dataset = new IngestedDatasetResponse();
        dataset.setId(datasetId);
        dataset.setRawData(objectMapper.writeValueAsString(records));
        when(ingestionClient.getDatasetById(datasetId)).thenReturn(dataset);

        // Stub rule-engine: a simple UPPERCASE rule on name
        RuleResponse rule = new RuleResponse();
        rule.setId(ruleId);
        rule.setName("Uppercase Name");
        rule.setRuleType("UPPERCASE");
        rule.setFieldName("name");
        rule.setPriority(1);
        rule.setActive(true);
        when(ruleEngineClient.getRulesByIds(any())).thenReturn(new ArrayList<>(List.of(rule)));
    }

    // ── Test 1: QUALITY_BLOCKED when quality FAIL and not skipped ──

    @Test
    void jobWithFailingQualityGate_shouldBeBlocked() throws Exception {
        QualityReport failReport = buildQualityReport(OverallStatus.FAIL, 40);
        when(dataQualityClient.validate(any(QualityValidateRequest.class))).thenReturn(failReport);

        ProcessingRequest request = new ProcessingRequest();
        request.setDatasetId(datasetId);
        request.setRuleIds(List.of(ruleId));
        request.setSkipQualityCheck(false);
        request.setQualityRuleSetId(qualityRuleSetId);

        ProcessingStatusResponse submitted = standardizationService.submitJob(request);

        // Wait for async to complete
        awaitJobTerminal(submitted.getJobId());

        ProcessingStatusResponse status = standardizationService.getJobStatus(submitted.getJobId());
        assertThat(status.getStatus()).isEqualTo("QUALITY_BLOCKED");
        assertThat(status.getQualityScore()).isEqualTo(40);
        assertThat(status.getQualityStatus()).isEqualTo("FAIL");
        assertThat(status.getQualityReportId()).isNotNull();
    }

    // ── Test 2: skipQualityCheck=true bypasses gate ──

    @Test
    void jobWithSkipQualityCheck_shouldProceedDespiteFail() throws Exception {
        QualityReport failReport = buildQualityReport(OverallStatus.FAIL, 40);
        when(dataQualityClient.validate(any(QualityValidateRequest.class))).thenReturn(failReport);

        ProcessingRequest request = new ProcessingRequest();
        request.setDatasetId(datasetId);
        request.setRuleIds(List.of(ruleId));
        request.setSkipQualityCheck(true);

        ProcessingStatusResponse submitted = standardizationService.submitJob(request);
        awaitJobTerminal(submitted.getJobId());

        ProcessingStatusResponse status = standardizationService.getJobStatus(submitted.getJobId());
        assertThat(status.getStatus()).isEqualTo("COMPLETED");
        assertThat(status.getQualityScore()).isEqualTo(40);
        assertThat(status.getQualityStatus()).isEqualTo("FAIL");
    }

    // ── Test 3: WARN does not block, qualityScore is stored ──

    @Test
    void jobWithWarnQuality_shouldCompleteAndStoreScore() throws Exception {
        QualityReport warnReport = buildQualityReport(OverallStatus.WARN, 75);
        when(dataQualityClient.validate(any(QualityValidateRequest.class))).thenReturn(warnReport);

        ProcessingRequest request = new ProcessingRequest();
        request.setDatasetId(datasetId);
        request.setRuleIds(List.of(ruleId));

        ProcessingStatusResponse submitted = standardizationService.submitJob(request);
        awaitJobTerminal(submitted.getJobId());

        ProcessingStatusResponse status = standardizationService.getJobStatus(submitted.getJobId());
        assertThat(status.getStatus()).isEqualTo("COMPLETED");
        assertThat(status.getQualityScore()).isEqualTo(75);
        assertThat(status.getQualityStatus()).isEqualTo("WARN");
    }

    // ── Test 4: GET /jobs/{id} returns quality fields ──

    @Test
    void getJobStatus_shouldReturnQualityFields() throws Exception {
        QualityReport passReport = buildQualityReport(OverallStatus.PASS, 98);
        when(dataQualityClient.validate(any(QualityValidateRequest.class))).thenReturn(passReport);

        ProcessingRequest request = new ProcessingRequest();
        request.setDatasetId(datasetId);
        request.setRuleIds(List.of(ruleId));

        ProcessingStatusResponse submitted = standardizationService.submitJob(request);
        awaitJobTerminal(submitted.getJobId());

        ProcessingStatusResponse status = standardizationService.getJobStatus(submitted.getJobId());
        assertThat(status.getQualityScore()).isEqualTo(98);
        assertThat(status.getQualityReportId()).isNotNull();
        assertThat(status.getQualityStatus()).isEqualTo("PASS");
    }

    // ── Test 5: Preview returns qualityReport even when blocked ──

    @Test
    void preview_qualityFail_shouldReturnBlockedWithReport() {
        QualityReport failReport = buildQualityReport(OverallStatus.FAIL, 30);
        when(dataQualityClient.validate(any(QualityValidateRequest.class))).thenReturn(failReport);

        ProcessingRequest request = new ProcessingRequest();
        request.setDatasetId(datasetId);
        request.setRuleIds(List.of(ruleId));
        request.setSkipQualityCheck(false);
        request.setQualityRuleSetId(qualityRuleSetId);

        StandardizedResultResponse preview = standardizationService.preview(request, 5);
        assertThat(preview.getQualityBlocked()).isTrue();
        assertThat(preview.getQualityReport()).isNotNull();
        assertThat(preview.getQualityReport().getOverallStatus()).isEqualTo(OverallStatus.FAIL);
        assertThat(preview.getStandardizedRecords()).isNull();
        assertThat(preview.getStatus()).isEqualTo("QUALITY_BLOCKED");
    }

    @Test
    void preview_qualityPass_shouldReturnDataWithReport() {
        QualityReport passReport = buildQualityReport(OverallStatus.PASS, 95);
        when(dataQualityClient.validate(any(QualityValidateRequest.class))).thenReturn(passReport);

        ProcessingRequest request = new ProcessingRequest();
        request.setDatasetId(datasetId);
        request.setRuleIds(List.of(ruleId));
        request.setQualityRuleSetId(qualityRuleSetId);

        StandardizedResultResponse preview = standardizationService.preview(request, 5);
        assertThat(preview.getQualityBlocked()).isFalse();
        assertThat(preview.getQualityReport()).isNotNull();
        assertThat(preview.getStandardizedRecords()).isNotNull().isNotEmpty();
        assertThat(preview.getStatus()).isEqualTo("PREVIEW");
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private QualityReport buildQualityReport(OverallStatus status, int score) {
        QualityReport report = new QualityReport();
        report.setReportId(UUID.randomUUID());
        report.setDatasetId(datasetId);
        report.setOverallStatus(status);
        report.setQualityScore(score);
        report.setTotalRecords(10);
        report.setDuplicateCount(0);
        report.setEvaluatedAt(Instant.now());

        ValidationRuleResult ruleResult = new ValidationRuleResult();
        ruleResult.setRuleId(UUID.randomUUID());
        ruleResult.setRuleName("NOT_NULL email");
        ruleResult.setColumnName("email");
        ruleResult.setValidationType("NOT_NULL");
        ruleResult.setPassed(status == OverallStatus.PASS);
        ruleResult.setFailRatePct(status == OverallStatus.PASS ? 0 : 10.0);
        ruleResult.setFailCount(status == OverallStatus.PASS ? 0 : 1);
        ruleResult.setSeverity(status == OverallStatus.FAIL ? "ERROR" : "WARNING");
        ruleResult.setMessage(status == OverallStatus.PASS ? "All values present" : "1 null value found in email");
        report.setRuleResults(List.of(ruleResult));

        return report;
    }

    private void awaitJobTerminal(UUID jobId) throws InterruptedException {
        Set<String> terminalStatuses = Set.of("COMPLETED", "FAILED", "QUALITY_BLOCKED");
        for (int i = 0; i < 50; i++) {
            ProcessingStatusResponse s = standardizationService.getJobStatus(jobId);
            if (terminalStatuses.contains(s.getStatus())) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Job " + jobId + " did not reach terminal status within 5 seconds");
    }
}





