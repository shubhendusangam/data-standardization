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
    void getDatasetById_notFound_shouldReturn500() throws Exception {
        mockMvc.perform(get("/api/ingestion/datasets/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
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
    void deleteDataset_notFound_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/api/ingestion/datasets/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
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

