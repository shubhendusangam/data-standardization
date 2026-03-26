package com.datastd.quality.exception;

import com.datastd.common.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest req) {
        log.warn("Resource not found: {}", ex.getMessage());
        String errorCode = deriveNotFoundCode(ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, errorCode, ex.getMessage(), req);
    }

    @ExceptionHandler(DataProcessingException.class)
    public ResponseEntity<ErrorResponse> handleDataProcessing(
            DataProcessingException ex, HttpServletRequest req) {
        log.error("Data processing error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                QualityErrorCode.DATA_PROCESSING_ERROR, ex.getMessage(), req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest req) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, QualityErrorCode.BAD_REQUEST,
                ex.getMessage(), req);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        String fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation failed: {}", fieldErrors);
        return buildResponse(HttpStatus.BAD_REQUEST, QualityErrorCode.VALIDATION_FAILED,
                "Validation failed — " + fieldErrors, req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(
            Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, QualityErrorCode.INTERNAL_ERROR,
                ex.getMessage(), req);
    }

    // ── helpers ──────────────────────────────────────────────────

    private String deriveNotFoundCode(String message) {
        if (message != null) {
            String lower = message.toLowerCase();
            if (lower.contains("rule set") || lower.contains("ruleset")) {
                return QualityErrorCode.RULESET_NOT_FOUND;
            }
            if (lower.contains("report")) {
                return QualityErrorCode.REPORT_NOT_FOUND;
            }
            if (lower.contains("alert")) {
                return QualityErrorCode.ALERT_CONFIG_NOT_FOUND;
            }
        }
        return QualityErrorCode.RULE_NOT_FOUND;
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status, String errorCode, String message, HttpServletRequest req) {
        ErrorResponse body = ErrorResponse.builder()
                .errorCode(errorCode)
                .message(message)
                .traceId(MDC.get("traceId"))
                .httpStatus(status.value())
                .path(req.getRequestURI())
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(status).body(body);
    }
}

