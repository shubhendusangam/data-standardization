package com.datastd.common.dto;

import java.time.Instant;

/**
 * Standardised error response DTO used across all services.
 * Every error response — 400, 404, 500, etc. — uses this shape so that
 * clients always receive a predictable JSON body that includes the traceId
 * for log correlation.
 */
public class ErrorResponse {

    private String errorCode;
    private String message;
    private String traceId;
    private int httpStatus;
    private String path;
    private Instant timestamp;

    public ErrorResponse() {}

    private ErrorResponse(Builder builder) {
        this.errorCode = builder.errorCode;
        this.message = builder.message;
        this.traceId = builder.traceId;
        this.httpStatus = builder.httpStatus;
        this.path = builder.path;
        this.timestamp = builder.timestamp;
    }

    public static Builder builder() {
        return new Builder();
    }

    // ── Getters & Setters ────────────────────────────────────────

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public int getHttpStatus() { return httpStatus; }
    public void setHttpStatus(int httpStatus) { this.httpStatus = httpStatus; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    // ── Builder ──────────────────────────────────────────────────

    public static class Builder {
        private String errorCode;
        private String message;
        private String traceId;
        private int httpStatus;
        private String path;
        private Instant timestamp;

        public Builder errorCode(String errorCode) { this.errorCode = errorCode; return this; }
        public Builder message(String message) { this.message = message; return this; }
        public Builder traceId(String traceId) { this.traceId = traceId; return this; }
        public Builder httpStatus(int httpStatus) { this.httpStatus = httpStatus; return this; }
        public Builder path(String path) { this.path = path; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }

        public ErrorResponse build() {
            return new ErrorResponse(this);
        }
    }
}

