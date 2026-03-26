package com.datastd.ingestion.exception;

/**
 * Error codes specific to the data-ingestion-service.
 */
public final class IngestionErrorCode {

    public static final String DATASET_NOT_FOUND = "DATASET_NOT_FOUND";
    public static final String FILE_PARSE_ERROR = "FILE_PARSE_ERROR";
    public static final String UNSUPPORTED_FILE_TYPE = "UNSUPPORTED_FILE_TYPE";
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private IngestionErrorCode() {}
}

