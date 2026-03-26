package com.datastd.rules.exception;

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
        request.setRequestURI("/api/rules/abc-123");
        MDC.put("traceId", "test-trace-id-002");
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void handleResourceNotFound_rule_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Rule not found with id: abc-123");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorCode()).isEqualTo("RULE_NOT_FOUND");
        assertThat(body.getMessage()).contains("abc-123");
        assertThat(body.getHttpStatus()).isEqualTo(404);
        assertThat(body.getPath()).isEqualTo("/api/rules/abc-123");
        assertThat(body.getTraceId()).isEqualTo("test-trace-id-002");
        assertThat(body.getTimestamp()).isNotNull();
    }

    @Test
    void handleResourceNotFound_ruleSet_returns404WithRulesetCode() {
        ResourceNotFoundException ex = new ResourceNotFoundException("RuleSet not found with id: xyz-456");

        ResponseEntity<ErrorResponse> response = handler.handleResourceNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorCode()).isEqualTo("RULESET_NOT_FOUND");
    }

    @Test
    void handleRuleConfigException_returns400() {
        RuleConfigException ex = new RuleConfigException("Missing 'pattern' field", "REGEX");

        ResponseEntity<ErrorResponse> response = handler.handleRuleConfigException(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorCode()).isEqualTo("INVALID_RULE_CONFIG");
        assertThat(body.getMessage()).contains("pattern");
        assertThat(body.getHttpStatus()).isEqualTo(400);
        assertThat(body.getTraceId()).isEqualTo("test-trace-id-002");
    }

    @Test
    void handleValidation_returns400() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "must not be blank"));
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ErrorResponse> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorCode()).isEqualTo("VALIDATION_FAILED");
        assertThat(body.getHttpStatus()).isEqualTo(400);
        assertThat(body.getTraceId()).isEqualTo("test-trace-id-002");
    }

    @Test
    void handleIllegalArgument_returns400() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid argument");

        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorCode()).isEqualTo("BAD_REQUEST");
        assertThat(body.getHttpStatus()).isEqualTo(400);
        assertThat(body.getTraceId()).isEqualTo("test-trace-id-002");
    }

    @Test
    void handleAll_returns500() {
        Exception ex = new Exception("Unexpected error");

        ResponseEntity<ErrorResponse> response = handler.handleAll(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(body.getHttpStatus()).isEqualTo(500);
        assertThat(body.getTraceId()).isEqualTo("test-trace-id-002");
    }
}

