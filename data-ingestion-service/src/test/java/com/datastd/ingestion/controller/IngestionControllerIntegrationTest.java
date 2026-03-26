package com.datastd.ingestion.controller;

import com.datastd.ingestion.entity.IngestedDataset;
import com.datastd.ingestion.repository.IngestedDatasetRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IngestionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IngestedDatasetRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    // ─── POST /api/ingestion/json ─────────────────────────────────

    @Test
    void ingestJson_shouldCreateDatasetAndReturn201() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "Test Dataset",
                "records", List.of(
                        Map.of("name", "Alice", "city", "NYC"),
                        Map.of("name", "Bob", "city", "LA")
                )
        ));

        mockMvc.perform(post("/api/ingestion/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Dataset"))
                .andExpect(jsonPath("$.recordCount").value(2))
                .andExpect(jsonPath("$.status").value("PARSED"));

        assertThat(repository.findAll()).hasSize(1);
    }

    @Test
    void ingestJson_missingName_shouldReturn400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "records", List.of(Map.of("a", "b"))
        ));

        mockMvc.perform(post("/api/ingestion/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ingestJson_emptyRecords_shouldReturn400() throws Exception {
        String body = """
                {"name": "Empty", "records": []}
                """;

        mockMvc.perform(post("/api/ingestion/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // ─── GET /api/ingestion/datasets ──────────────────────────────

    @Test
    void getAllDatasets_shouldReturnList() throws Exception {
        // Ingest two datasets
        ingestSampleDataset("Dataset 1");
        ingestSampleDataset("Dataset 2");

        mockMvc.perform(get("/api/ingestion/datasets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    // ─── GET /api/ingestion/datasets/{id} ─────────────────────────

    @Test
    void getDatasetById_shouldReturnDatasetWithRawData() throws Exception {
        UUID id = ingestSampleDataset("Lookup Test");

        mockMvc.perform(get("/api/ingestion/datasets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Lookup Test"))
                .andExpect(jsonPath("$.rawData").isNotEmpty());
    }

    @Test
    void getDatasetById_notFound_shouldReturn404WithErrorResponse() throws Exception {
        mockMvc.perform(get("/api/ingestion/datasets/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("DATASET_NOT_FOUND"))
                .andExpect(jsonPath("$.httpStatus").value(404))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // ─── DELETE /api/ingestion/datasets/{id} ──────────────────────

    @Test
    void deleteDataset_shouldRemoveAndReturn204() throws Exception {
        UUID id = ingestSampleDataset("Delete Me");

        mockMvc.perform(delete("/api/ingestion/datasets/" + id))
                .andExpect(status().isNoContent());

        assertThat(repository.findById(id)).isEmpty();
    }

    @Test
    void deleteDataset_notFound_shouldReturn404WithErrorResponse() throws Exception {
        mockMvc.perform(delete("/api/ingestion/datasets/" + UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("DATASET_NOT_FOUND"))
                .andExpect(jsonPath("$.httpStatus").value(404))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    // ─── POST /api/ingestion/upload (CSV with warnings) ────────────

    @Test
    void uploadCsv_withBadRows_shouldReturnParseWarnings() throws Exception {
        // CSV with 3 columns in header, but row 3 has only 1 column
        String csv = "name,age,city\nAlice,30,NYC\nBob\nCharlie,35,Chicago\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/ingestion/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PARSED"))
                .andExpect(jsonPath("$.skippedRowCount").value(1))
                .andExpect(jsonPath("$.parseWarnings", hasSize(1)))
                .andExpect(jsonPath("$.parseWarnings[0].rowIndex").value(3))
                .andExpect(jsonPath("$.parseWarnings[0].reason", containsString("columns, expected")))
                .andExpect(jsonPath("$.warningsTruncated").value(false));
    }

    @Test
    void uploadCsv_allValid_shouldReturnEmptyWarnings() throws Exception {
        String csv = "name,age\nAlice,30\nBob,25\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "valid.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/ingestion/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PARSED"))
                .andExpect(jsonPath("$.parsedRowCount").value(2))
                .andExpect(jsonPath("$.skippedRowCount").value(0))
                .andExpect(jsonPath("$.parseWarnings", hasSize(0)))
                .andExpect(jsonPath("$.warningsTruncated").value(false));
    }

    // ─── POST /api/ingestion/upload (validation) ──────────────────

    @Test
    void uploadFile_emptyFile_shouldReturn400() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.csv", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/api/ingestion/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.message").value("Uploaded file is empty"));
    }

    @Test
    void uploadFile_unsupportedContentType_shouldReturn415() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "fake-pdf-content".getBytes());

        mockMvc.perform(multipart("/api/ingestion/upload").file(file))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_FILE_TYPE"))
                .andExpect(jsonPath("$.message", containsString("application/pdf")));
    }

    // ─── Helper ───────────────────────────────────────────────────

    private UUID ingestSampleDataset(String name) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "name", name,
                "records", List.of(Map.of("col", "val"))
        ));

        String response = mockMvc.perform(post("/api/ingestion/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(objectMapper.readTree(response).get("datasetId").asText());
    }
}

