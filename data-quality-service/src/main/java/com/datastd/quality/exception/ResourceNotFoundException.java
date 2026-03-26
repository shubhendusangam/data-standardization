package com.datastd.quality.exception;

/**
 * Thrown when a requested resource (rule, rule set, report, alert config) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

