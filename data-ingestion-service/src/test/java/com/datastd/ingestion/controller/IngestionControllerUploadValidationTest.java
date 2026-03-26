package com.datastd.ingestion.controller;

import com.datastd.ingestion.dto.FileParseResult;
import com.datastd.ingestion.dto.UploadResult;
import com.datastd.ingestion.entity.IngestedDataset;
import com.datastd.ingestion.entity.IngestedDataset.DatasetStatus;
import com.datastd.ingestion.entity.IngestedDataset.SourceType;
import com.datastd.ingestion.exception.FileSizeLimitExceededException;
import com.datastd.ingestion.exception.UnsupportedFileTypeException;
import com.datastd.ingestion.service.IngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the upload validation logic in {@link IngestionController}.
 * <p>
 * These tests instantiate the controller directly with mocked dependencies
 * and verify that size, emptiness, and content-type checks throw the
 * correct exceptions <em>before</em> the service layer is ever called.
 */
@ExtendWith(MockitoExtension.class)
class IngestionControllerUploadValidationTest {

    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024; // 50 MB

    private static final List<String> SUPPORTED_TYPES = List.of(
            "text/csv",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    @Mock
    private IngestionService ingestionService;

    private IngestionController controller;

    @BeforeEach
    void setUp() {
        controller = new IngestionController(ingestionService, MAX_FILE_SIZE_BYTES, SUPPORTED_TYPES);
    }

    // ─── Empty file ──────────────────────────────────────────────

    @Test
    void uploadFile_emptyFile_shouldThrowIllegalArgumentException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> controller.uploadFile(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Uploaded file is empty");

        verifyNoInteractions(ingestionService);
    }

    // ─── Oversized file ──────────────────────────────────────────

    @Test
    void uploadFile_exceedsMaxSize_shouldThrowFileSizeLimitExceededException() {
        long oversized = MAX_FILE_SIZE_BYTES + 1;
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(oversized);

        assertThatThrownBy(() -> controller.uploadFile(file))
                .isInstanceOf(FileSizeLimitExceededException.class)
                .hasMessageContaining("exceeds the maximum allowed size");

        verifyNoInteractions(ingestionService);
    }

    @Test
    void uploadFile_exactlyAtMaxSize_shouldNotThrowFileSizeException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(MAX_FILE_SIZE_BYTES); // exactly at limit
        when(file.getContentType()).thenReturn("text/csv");

        IngestedDataset dataset = buildParsedDataset();
        FileParseResult parseResult = new FileParseResult(List.of(), List.of());
        when(ingestionService.uploadFile(file)).thenReturn(new UploadResult(dataset, parseResult));

        ResponseEntity<?> response = controller.uploadFile(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(ingestionService).uploadFile(file);
    }

    // ─── Unsupported content type ────────────────────────────────

    @Test
    void uploadFile_unsupportedContentType_shouldThrowUnsupportedFileTypeException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn("application/pdf");

        assertThatThrownBy(() -> controller.uploadFile(file))
                .isInstanceOf(UnsupportedFileTypeException.class)
                .hasMessageContaining("application/pdf")
                .hasMessageContaining("Allowed: CSV, XLS, XLSX");

        verifyNoInteractions(ingestionService);
    }

    @Test
    void uploadFile_nullContentType_shouldThrowUnsupportedFileTypeException() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(1024L);
        when(file.getContentType()).thenReturn(null);

        assertThatThrownBy(() -> controller.uploadFile(file))
                .isInstanceOf(UnsupportedFileTypeException.class)
                .hasMessageContaining("null");

        verifyNoInteractions(ingestionService);
    }

    // ─── Valid CSV ────────────────────────────────────────────────

    @Test
    void uploadFile_validCsvWithinSizeLimit_shouldDelegateToServiceAndReturn201() {
        long oneMb = 1024L * 1024; // 1 MB
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(oneMb);
        when(file.getContentType()).thenReturn("text/csv");

        IngestedDataset dataset = buildParsedDataset();
        FileParseResult parseResult = new FileParseResult(List.of(), List.of());
        when(ingestionService.uploadFile(file)).thenReturn(new UploadResult(dataset, parseResult));

        ResponseEntity<?> response = controller.uploadFile(file);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(ingestionService).uploadFile(file);
    }

    // ─── formatSize helper ───────────────────────────────────────

    @Test
    void formatSize_shouldReturnHumanReadableString() {
        assertThat(IngestionController.formatSize(500)).isEqualTo("500 B");
        assertThat(IngestionController.formatSize(1024 * 1024)).isEqualTo("1.0 MB");
        assertThat(IngestionController.formatSize(50L * 1024 * 1024)).isEqualTo("50.0 MB");
        assertThat(IngestionController.formatSize(75L * 1024 * 1024 + 300 * 1024)).isEqualTo("75.3 MB");
    }

    // ─── Helper ──────────────────────────────────────────────────

    private IngestedDataset buildParsedDataset() {
        IngestedDataset dataset = new IngestedDataset();
        dataset.setId(UUID.randomUUID());
        dataset.setName("test.csv");
        dataset.setSourceType(SourceType.CSV);
        dataset.setStatus(DatasetStatus.PARSED);
        dataset.setRecordCount(10);
        return dataset;
    }
}

