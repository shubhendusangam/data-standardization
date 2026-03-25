package com.datastd.common.dto;

/**
 * Inferred column data type based on value analysis.
 */
public enum InferredType {
    STRING,
    INTEGER,
    DECIMAL,
    BOOLEAN,
    DATE,
    MIXED,
    UNKNOWN
}

