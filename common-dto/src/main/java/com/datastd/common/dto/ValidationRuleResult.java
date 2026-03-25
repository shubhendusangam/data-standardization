package com.datastd.common.dto;

import java.util.UUID;

/**
 * Result of applying a single validation rule during a quality check.
 */
public class ValidationRuleResult {

    private UUID ruleId;
    private String ruleName;
    private String columnName;
    private String validationType;
    private boolean passed;
    private double failRatePct;
    private int failCount;
    private String severity;
    private String message;

    public ValidationRuleResult() {}

    // Getters and Setters

    public UUID getRuleId() {
        return ruleId;
    }

    public void setRuleId(UUID ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getValidationType() {
        return validationType;
    }

    public void setValidationType(String validationType) {
        this.validationType = validationType;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public double getFailRatePct() {
        return failRatePct;
    }

    public void setFailRatePct(double failRatePct) {
        this.failRatePct = failRatePct;
    }

    public int getFailCount() {
        return failCount;
    }

    public void setFailCount(int failCount) {
        this.failCount = failCount;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

