package com.datastd.ingestion.exception;

/**
 * Thrown when an uploaded file has a content type that is not supported.
 * Mapped to HTTP 415 (Unsupported Media Type) in GlobalExceptionHandler.
 */
public class UnsupportedFileTypeException extends RuntimeException {

    public UnsupportedFileTypeException(String message) {
        super(message);
    }
}

