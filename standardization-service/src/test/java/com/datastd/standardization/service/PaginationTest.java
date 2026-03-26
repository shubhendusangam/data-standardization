package com.datastd.standardization.service;

import com.datastd.common.dto.PagedResult;
import com.datastd.standardization.entity.ProcessingJob;
import com.datastd.standardization.entity.ProcessingJob.JobStatus;
import com.datastd.standardization.repository.ProcessingJobRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for paginated GET /api/standardization/jobs/{jobId}/result.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaginationTest {

    @MockBean
    private com.datastd.standardization.client.IngestionServiceClient ingestionClient;

    @MockBean
    private com.datastd.standardization.client.RuleEngineClient ruleEngineClient;

    @MockBean
    private com.datastd.standardization.client.DataQualityClient dataQualityClient;

    @Autowired
    private ProcessingJobRepository jobRepository;

    @Autowired
    private StandardizationService standardizationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mockMvc;

    private UUID completedJobId;

    @BeforeEach
    void setUp() throws Exception {
        jobRepository.deleteAll();

        // Create a COMPLETED job with 250 records
        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 0; i < 250; i++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", i);
            row.put("name", "Record-" + i);
            records.add(row);
        }

        ProcessingJob job = new ProcessingJob();
        job.setDatasetId(UUID.randomUUID());
        job.setStatus(JobStatus.COMPLETED);
        job.setProcessedRecords(250);
        job.setTotalRecords(250);
        job.setResultData(objectMapper.writeValueAsString(records));

        ProcessingJob saved = jobRepository.save(job);
        completedJobId = saved.getId();
    }

    // ── Unit test 1: page 0, size 100 on 250-record job ─────────

    @Test
    void getJobResult_page0_size100_returns_first100_of_250() {
        PagedResult result = standardizationService.getJobResult(completedJobId, 0, 100);

        assertThat(result.getRecords()).hasSize(100);
        assertThat(result.getTotalRecords()).isEqualTo(250);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getPage()).isEqualTo(0);
        assertThat(result.getSize()).isEqualTo(100);
        assertThat(result.isHasNext()).isTrue();
        assertThat(result.isHasPrevious()).isFalse();

        // Verify first record
        assertThat(result.getRecords().get(0).get("id")).isEqualTo(0);
        // Verify last record on this page
        assertThat(result.getRecords().get(99).get("id")).isEqualTo(99);
    }

    // ── Unit test 2: page 2, size 100 → last page ──────────────

    @Test
    void getJobResult_page2_size100_returns_last50_hasNextFalse() {
        PagedResult result = standardizationService.getJobResult(completedJobId, 2, 100);

        assertThat(result.getRecords()).hasSize(50);
        assertThat(result.getTotalRecords()).isEqualTo(250);
        assertThat(result.getTotalPages()).isEqualTo(3);
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.isHasPrevious()).isTrue();

        // Verify records 200–249
        assertThat(result.getRecords().get(0).get("id")).isEqualTo(200);
        assertThat(result.getRecords().get(49).get("id")).isEqualTo(249);
    }

    // ── Unit test 3: size > 1000 → controller returns 400 ──────

    @Test
    void getJobResult_sizeExceeds1000_returns400() throws Exception {
        mockMvc.perform(get("/api/standardization/jobs/{jobId}/result", completedJobId)
                        .param("page", "0")
                        .param("size", "1001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size must not exceed 1000"));
    }

    // ── size=0 → 400 ──────────────────────────────────────────

    @Test
    void getJobResult_sizeZero_returns400() throws Exception {
        mockMvc.perform(get("/api/standardization/jobs/{jobId}/result", completedJobId)
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("size must be at least 1"));
    }

    // ── Integration test: GET ?page=0&size=5 returns exactly 5 records ──

    @Test
    void getJobResult_http_page0_size5_returnsExactly5Records() throws Exception {
        mockMvc.perform(get("/api/standardization/jobs/{jobId}/result", completedJobId)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records.length()").value(5))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalRecords").value(250))
                .andExpect(jsonPath("$.totalPages").value(50))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(false));
    }

    // ── Content-Range header is present ─────────────────────────

    @Test
    void getJobResult_http_containsContentRangeHeader() throws Exception {
        mockMvc.perform(get("/api/standardization/jobs/{jobId}/result", completedJobId)
                        .param("page", "0")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Range", "records 0-99/250"));
    }

    @Test
    void getJobResult_http_lastPage_contentRangeCorrect() throws Exception {
        mockMvc.perform(get("/api/standardization/jobs/{jobId}/result", completedJobId)
                        .param("page", "2")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Range", "records 200-249/250"));
    }

    // ── totalRecords is always the full dataset count ───────────

    @Test
    void getJobResult_totalRecords_alwaysFullCount() {
        PagedResult page0 = standardizationService.getJobResult(completedJobId, 0, 10);
        PagedResult page5 = standardizationService.getJobResult(completedJobId, 5, 10);

        assertThat(page0.getTotalRecords()).isEqualTo(250);
        assertThat(page5.getTotalRecords()).isEqualTo(250);
        assertThat(page0.getRecords()).hasSize(10);
        assertThat(page5.getRecords()).hasSize(10);
    }

    // ── page=1, size=100 returns records 100–199 ────────────────

    @Test
    void getJobResult_page1_size100_returnsRecords100to199() {
        PagedResult result = standardizationService.getJobResult(completedJobId, 1, 100);

        assertThat(result.getRecords()).hasSize(100);
        assertThat(result.getRecords().get(0).get("id")).isEqualTo(100);
        assertThat(result.getRecords().get(99).get("id")).isEqualTo(199);
        assertThat(result.isHasNext()).isTrue();
        assertThat(result.isHasPrevious()).isTrue();
    }

    // ── Defaults: no params → page=0, size=100 ─────────────────

    @Test
    void getJobResult_http_defaultParams() throws Exception {
        mockMvc.perform(get("/api/standardization/jobs/{jobId}/result", completedJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(100))
                .andExpect(jsonPath("$.records.length()").value(100));
    }

    // ── Response shape has no job-metadata fields ───────────────

    @Test
    void getJobResult_http_responseShape_noJobMetadata() throws Exception {
        mockMvc.perform(get("/api/standardization/jobs/{jobId}/result", completedJobId)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records").isArray())
                .andExpect(jsonPath("$.page").isNumber())
                .andExpect(jsonPath("$.size").isNumber())
                .andExpect(jsonPath("$.totalRecords").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.hasNext").isBoolean())
                .andExpect(jsonPath("$.hasPrevious").isBoolean())
                // Must NOT include job metadata
                .andExpect(jsonPath("$.jobId").doesNotExist())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.errorLog").doesNotExist())
                .andExpect(jsonPath("$.standardizedRecords").doesNotExist());
    }

    // ── Job not found → 404 ─────────────────────────────────────

    @Test
    void getJobResult_unknownJob_returns404() throws Exception {
        UUID unknownId = UUID.randomUUID();
        mockMvc.perform(get("/api/standardization/jobs/{jobId}/result", unknownId)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isNotFound());
    }

    // ── page beyond range → empty records, correct metadata ────

    @Test
    void getJobResult_pageBeyondRange_returnsEmptyRecords() {
        PagedResult result = standardizationService.getJobResult(completedJobId, 999, 100);

        assertThat(result.getRecords()).isEmpty();
        assertThat(result.getTotalRecords()).isEqualTo(250);
        assertThat(result.isHasNext()).isFalse();
    }

    // ── Negative page → 400 ────────────────────────────────────

    @Test
    void getJobResult_negativePage_returns400() throws Exception {
        mockMvc.perform(get("/api/standardization/jobs/{jobId}/result", completedJobId)
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest());
    }

    // ── No change to other endpoints ────────────────────────────

    @Test
    void otherEndpoints_notAffected() throws Exception {
        // GET /jobs/{jobId} still works
        mockMvc.perform(get("/api/standardization/jobs/{jobId}", completedJobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(completedJobId.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // GET /jobs still works
        mockMvc.perform(get("/api/standardization/jobs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}

