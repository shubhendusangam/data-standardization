package com.datastd.ingestion.controller;

import com.datastd.ingestion.repository.IngestedDatasetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for file upload size-limit enforcement.
 * <p>
 * Uses a very low {@code max-file-size-bytes} (100 bytes) so that a normal-sized
 * test payload triggers the 413 path without allocating 50 MB+ of memory.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "datastandard.ingestion.max-file-size-bytes=100",
        "spring.servlet.multipart.max-file-size=50MB",
        "spring.servlet.multipart.max-request-size=52MB"
})
class IngestionUploadLimitsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IngestedDatasetRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void uploadFile_exceedsSizeLimit_shouldReturn413WithFileTooLarge() throws Exception {
        // 200 bytes of CSV data — exceeds the 100-byte limit set via @TestPropertySource
        String csv = "name,email,city,country,phone\n"
                + "Alice,alice@example.com,New York,US,555-0100\n"
                + "Bob,bob@example.com,Los Angeles,US,555-0101\n"
                + "Charlie,charlie@example.com,Chicago,US,555-0102\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "oversized.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/ingestion/upload").file(file))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.errorCode").value("FILE_TOO_LARGE"))
                .andExpect(jsonPath("$.httpStatus").value(413))
                .andExpect(jsonPath("$.message", containsString("exceeds the maximum allowed size")))
                .andExpect(jsonPath("$.path").value("/api/ingestion/upload"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void uploadFile_withinSizeLimit_shouldReturn201() throws Exception {
        // 26 bytes of CSV data — well within the 100-byte limit
        String csv = "name,age\nAlice,30\nBob,25\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "small.csv", "text/csv", csv.getBytes());

        mockMvc.perform(multipart("/api/ingestion/upload").file(file))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PARSED"));
    }
}

