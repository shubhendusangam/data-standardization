package com.datastd.standardization.exception;

/**
 * Error codes specific to the standardization-service.
 */
public final class StandardizationErrorCode {

    public static final String JOB_NOT_FOUND = "JOB_NOT_FOUND";
    public static final String DATASET_UNAVAILABLE = "DATASET_UNAVAILABLE";
    public static final String RULESET_UNAVAILABLE = "RULESET_UNAVAILABLE";
    public static final String JOB_ALREADY_RUNNING = "JOB_ALREADY_RUNNING";
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private StandardizationErrorCode() {}
}

