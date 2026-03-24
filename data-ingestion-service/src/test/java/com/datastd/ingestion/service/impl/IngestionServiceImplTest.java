package com.datastd.ingestion.service.impl;

import com.datastd.ingestion.dto.JsonDataRequest;
import com.datastd.ingestion.entity.IngestedDataset;
import com.datastd.ingestion.entity.IngestedDataset.DatasetStatus;
import com.datastd.ingestion.entity.IngestedDataset.SourceType;
import com.datastd.ingestion.repository.IngestedDatasetRepository;
import com.datastd.ingestion.service.FileParserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionServiceImplTest {

    @Mock
    private IngestedDatasetRepository repository;

    @Mock
    private FileParserService fileParserService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private IngestionServiceImpl ingestionService;

    @BeforeEach
    void setUp() {
        // Mockito handles injection via @InjectMocks
    }

    // ─── ingestJsonData ───────────────────────────────────────────

    @Test
    void ingestJsonData_shouldSaveDatasetWithParsedStatus() {
        // Arrange
        List<Map<String, Object>> records = List.of(
                Map.of("name", "Alice", "age", 30),
                Map.of("name", "Bob", "age", 25)
        );
        JsonDataRequest request = new JsonDataRequest("test-data", records);

        when(repository.save(any(IngestedDataset.class)))
                .thenAnswer(inv -> {
                    IngestedDataset d = inv.getArgument(0);
                    d.setId(UUID.randomUUID());
                    return d;
                });

        // Act
        IngestedDataset result = ingestionService.ingestJsonData(request);

        // Assert
        assertThat(result.getName()).isEqualTo("test-data");
        assertThat(result.getSourceType()).isEqualTo(SourceType.API);
        assertThat(result.getStatus()).isEqualTo(DatasetStatus.PARSED);
        assertThat(result.getRecordCount()).isEqualTo(2);
        assertThat(result.getRawData()).contains("Alice");

        verify(repository).save(any(IngestedDataset.class));
    }

    @Test
    void ingestJsonData_shouldSetRecordCountCorrectly() {
        List<Map<String, Object>> records = List.of(
                Map.of("x", 1), Map.of("x", 2), Map.of("x", 3)
        );
        JsonDataRequest request = new JsonDataRequest("three-records", records);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IngestedDataset result = ingestionService.ingestJsonData(request);

        assertThat(result.getRecordCount()).isEqualTo(3);
    }

    // ─── uploadFile ───────────────────────────────────────────────

    @Test
    void uploadFile_csv_shouldParseCsvFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "data.csv", "text/csv", "name,age\nAlice,30".getBytes());

        List<Map<String, String>> parsed = List.of(Map.of("name", "Alice", "age", "30"));
        when(fileParserService.parseCsv(any(InputStream.class))).thenReturn(parsed);
        when(repository.save(any())).thenAnswer(inv -> {
            IngestedDataset d = inv.getArgument(0);
            d.setId(UUID.randomUUID());
            return d;
        });

        IngestedDataset result = ingestionService.uploadFile(file);

        assertThat(result.getSourceType()).isEqualTo(SourceType.CSV);
        assertThat(result.getStatus()).isEqualTo(DatasetStatus.PARSED);
        assertThat(result.getRecordCount()).isEqualTo(1);
        verify(fileParserService).parseCsv(any());
    }

    @Test
    void uploadFile_excel_shouldParseExcelFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "data.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy".getBytes());

        List<Map<String, String>> parsed = List.of(Map.of("col", "val"));
        when(fileParserService.parseExcel(any(InputStream.class))).thenReturn(parsed);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        IngestedDataset result = ingestionService.uploadFile(file);

        assertThat(result.getSourceType()).isEqualTo(SourceType.EXCEL);
        assertThat(result.getStatus()).isEqualTo(DatasetStatus.PARSED);
        verify(fileParserService).parseExcel(any());
    }

    @Test
    void uploadFile_unsupportedFormat_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "data.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> ingestionService.uploadFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported file format");
    }

    @Test
    void uploadFile_nullFilename_shouldThrow() {
        MockMultipartFile file = new MockMultipartFile(
                "file", null, "text/csv", "data".getBytes());

        assertThatThrownBy(() -> ingestionService.uploadFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File name is required");
    }

    // ─── getDatasetById ───────────────────────────────────────────

    @Test
    void getDatasetById_found_shouldReturnDataset() {
        UUID id = UUID.randomUUID();
        IngestedDataset ds = new IngestedDataset();
        ds.setId(id);
        ds.setName("test");
        when(repository.findById(id)).thenReturn(Optional.of(ds));

        IngestedDataset result = ingestionService.getDatasetById(id);

        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    void getDatasetById_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ingestionService.getDatasetById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // ─── deleteDataset ────────────────────────────────────────────

    @Test
    void deleteDataset_exists_shouldDelete() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);

        ingestionService.deleteDataset(id);

        verify(repository).deleteById(id);
    }

    @Test
    void deleteDataset_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> ingestionService.deleteDataset(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    // ─── getAllDatasets ────────────────────────────────────────────

    @Test
    void getAllDatasets_shouldReturnAllOrderedByCreatedAt() {
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(new IngestedDataset()));

        List<IngestedDataset> result = ingestionService.getAllDatasets();

        assertThat(result).hasSize(1);
        verify(repository).findAllByOrderByCreatedAtDesc();
    }
}

