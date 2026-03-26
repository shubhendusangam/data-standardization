package com.datastd.quality.exception;

/**
 * Thrown when data processing or serialization fails in the quality service.
 */
public class DataProcessingException extends RuntimeException {

    public DataProcessingException(String message) {
        super(message);
    }

    public DataProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

