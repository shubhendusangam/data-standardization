package com.datastd.standardization.exception;

/**
 * Thrown when a requested resource (job, etc.) is not found.
 * Mapped to HTTP 404 in GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

