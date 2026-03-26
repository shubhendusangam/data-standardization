package com.datastd.rules.exception;

/**
 * Thrown when ruleConfig JSON is invalid for the given ruleType.
 * Mapped to HTTP 400 in GlobalExceptionHandler.
 */
public class RuleConfigException extends RuntimeException {

    private final String ruleType;

    public RuleConfigException(String message) {
        this(message, null);
    }

    public RuleConfigException(String message, String ruleType) {
        super(message);
        this.ruleType = ruleType;
    }

    public String getRuleType() {
        return ruleType;
    }
}

