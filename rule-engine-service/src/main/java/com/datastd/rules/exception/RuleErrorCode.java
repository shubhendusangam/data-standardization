package com.datastd.rules.exception;

/**
 * Error codes specific to the rule-engine-service.
 */
public final class RuleErrorCode {

    public static final String RULE_NOT_FOUND = "RULE_NOT_FOUND";
    public static final String RULESET_NOT_FOUND = "RULESET_NOT_FOUND";
    public static final String INVALID_RULE_CONFIG = "INVALID_RULE_CONFIG";
    public static final String RULE_TYPE_UNKNOWN = "RULE_TYPE_UNKNOWN";
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private RuleErrorCode() {}
}

