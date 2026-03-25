package com.datastd.quality.entity;

/**
 * Types of validation checks that can be applied to a column.
 */
public enum ValidationType {
    NOT_NULL,        // null rate must be below threshold
    NOT_EMPTY,       // blank string rate below threshold
    REGEX_MATCH,     // all values match pattern
    ALLOWED_VALUES,  // all values in allowed set
    NUMERIC_RANGE,   // all numeric values within [min, max]
    MIN_LENGTH,      // string length >= min
    MAX_LENGTH,      // string length <= max
    UNIQUE,          // no duplicate values in column
    CUSTOM_SQL       // reserved for future use
}

