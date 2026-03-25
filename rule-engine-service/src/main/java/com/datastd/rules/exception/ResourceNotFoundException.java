package com.datastd.rules.exception;

/**
 * Thrown when a requested resource (rule, rule set, etc.) is not found.
 * Mapped to HTTP 404 in GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

