package com.datastd.standardization.exception;

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

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRequestURI("/api/standardization/jobs/abc-123");
        MDC.put("traceId", "test-trace-id-003");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void handleResourceNotFound_returns404WithCorrectBody() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Job not found with id: abc-123");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorCode()).isEqualTo("JOB_NOT_FOUND");
        assertThat(body.getMessage()).contains("abc-123");
        assertThat(body.getHttpStatus()).isEqualTo(404);
        assertThat(body.getPath()).isEqualTo("/api/standardization/jobs/abc-123");
        assertThat(body.getTraceId()).isEqualTo("test-trace-id-003");
        assertThat(body.getTimestamp()).isNotNull();
    }

    @Test
    void handleValidation_returns400() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "datasetId", "must not be null"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(body.getMessage()).contains("datasetId");
        assertThat(body.getHttpStatus()).isEqualTo(400);
        assertThat(body.getTraceId()).isEqualTo("test-trace-id-003");
    }

    @Test
    void handleIllegalArgument_returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("Either ruleSetId or ruleIds must be provided");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorCode()).isEqualTo("BAD_REQUEST");
        assertThat(body.getMessage()).contains("ruleSetId");
        assertThat(body.getHttpStatus()).isEqualTo(400);
        assertThat(body.getTraceId()).isEqualTo("test-trace-id-003");
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
        assertThat(body.getTraceId()).isEqualTo("test-trace-id-003");
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

