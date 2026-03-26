package com.datastd.ingestion.exception;

import com.datastd.common.dto.ErrorResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/ingestion/datasets/abc-123");
        MDC.put("traceId", "test-trace-id-001");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void handleResourceNotFound_returns404WithCorrectBody() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Dataset not found with id: abc-123");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorCode()).isEqualTo("DATASET_NOT_FOUND");
        assertThat(body.getMessage()).contains("abc-123");
        assertThat(body.getHttpStatus()).isEqualTo(404);
        assertThat(body.getPath()).isEqualTo("/api/ingestion/datasets/abc-123");
        assertThat(body.getTraceId()).isEqualTo("test-trace-id-001");
        assertThat(body.getTimestamp()).isNotNull();
    }

    @Test
    void handleIllegalArgument_returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("File name is required");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorCode()).isEqualTo("BAD_REQUEST");
        assertThat(body.getMessage()).isEqualTo("File name is required");
        assertThat(body.getHttpStatus()).isEqualTo(400);
        assertThat(body.getTraceId()).isEqualTo("test-trace-id-001");
    }

    @Test
    void handleValidation_returns400WithFieldErrors() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "must not be blank"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(body.getMessage()).contains("name");
        assertThat(body.getHttpStatus()).isEqualTo(400);
        assertThat(body.getTraceId()).isEqualTo("test-trace-id-001");
    }

    @Test
    void handleMaxUploadSize_returns413() {
        MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(1024);

        ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSize(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorCode()).isEqualTo("FILE_TOO_LARGE");
        assertThat(body.getMessage()).isEqualTo("Uploaded file exceeds the 50 MB size limit");
        assertThat(body.getHttpStatus()).isEqualTo(413);
        assertThat(body.getTraceId()).isEqualTo("test-trace-id-001");
    }

    @Test
    void handleFileSizeLimit_returns413WithDetailedMessage() {
        long actualSize = 75L * 1024 * 1024; // 75 MB
        long maxSize = 50L * 1024 * 1024;    // 50 MB
        FileSizeLimitExceededException ex =
                new FileSizeLimitExceededException("File size 75.0 MB exceeds the maximum allowed size of 50.0 MB",
                        actualSize, maxSize);

        ResponseEntity<ErrorResponse> response = handler.handleFileSizeLimit(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorCode()).isEqualTo("FILE_TOO_LARGE");
        assertThat(body.getMessage()).contains("75.0 MB");
        assertThat(body.getMessage()).contains("50.0 MB");
        assertThat(body.getHttpStatus()).isEqualTo(413);
        assertThat(body.getTraceId()).isEqualTo("test-trace-id-001");
    }

    @Test
    void handleUnsupportedFileType_returns415() {
        UnsupportedFileTypeException ex =
                new UnsupportedFileTypeException("Unsupported file type: application/pdf. Allowed: CSV, XLS, XLSX");

        ResponseEntity<ErrorResponse> response = handler.handleUnsupportedFileType(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorCode()).isEqualTo("UNSUPPORTED_FILE_TYPE");
        assertThat(body.getMessage()).contains("application/pdf");
        assertThat(body.getHttpStatus()).isEqualTo(415);
        assertThat(body.getTraceId()).isEqualTo("test-trace-id-001");
    }

    @Test
    void handleAll_returns500() {
        Exception ex = new Exception("Something went wrong");

        ResponseEntity<ErrorResponse> response = handler.handleAll(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.getHttpStatus()).isEqualTo(500);
        assertThat(body.getTraceId()).isEqualTo("test-trace-id-001");
    }

    @Test
    void traceId_isNull_whenMdcIsEmpty() {
        MDC.clear();
        ResourceNotFoundException ex = new ResourceNotFoundException("not found");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, request);

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getTraceId()).isNull();
    }
}

