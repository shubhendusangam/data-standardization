package com.datastd.ingestion.exception;

/**
 * Thrown when an uploaded file exceeds the allowed size limit.
 * Mapped to HTTP 413 (Payload Too Large) in GlobalExceptionHandler.
 */
public class FileSizeLimitExceededException extends RuntimeException {

    private final long actualSize;
    private final long maxSize;

    public FileSizeLimitExceededException(String message, long actualSize, long maxSize) {
        super(message);
        this.actualSize = actualSize;
        this.maxSize = maxSize;
    }

    public long getActualSize() {
        return actualSize;
    }

    public long getMaxSize() {
        return maxSize;
    }
}

