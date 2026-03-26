package com.datastd.standardization.exception;

/**
 * Thrown by {@link com.datastd.standardization.service.rules.RuleApplier} implementations
 * when a value cannot be transformed by the rule.
 * <p>
 * Caught by {@link com.datastd.standardization.service.rules.RuleExecutionEngine} so that
 * the record retains its original value and the error is captured — without aborting the
 * entire job.
 */
public class RuleApplicationException extends RuntimeException {

    public RuleApplicationException(String message) {
        super(message);
    }

    public RuleApplicationException(String message, Throwable cause) {
        super(message, cause);
    }
}

