package com.datastd.quality.exception;

/**
 * Error codes specific to the data-quality-service.
 */
public final class QualityErrorCode {

    public static final String RULE_NOT_FOUND = "QUALITY_RULE_NOT_FOUND";
    public static final String RULESET_NOT_FOUND = "QUALITY_RULESET_NOT_FOUND";
    public static final String REPORT_NOT_FOUND = "QUALITY_REPORT_NOT_FOUND";
    public static final String ALERT_CONFIG_NOT_FOUND = "ALERT_CONFIG_NOT_FOUND";
    public static final String INVALID_RULE_IDS = "INVALID_RULE_IDS";
    public static final String DATA_PROCESSING_ERROR = "DATA_PROCESSING_ERROR";
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private QualityErrorCode() {}
}

