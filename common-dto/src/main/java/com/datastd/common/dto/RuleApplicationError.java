package com.datastd.common.dto;

/**
 * Captures per-record, per-rule error details when a rule fails to transform a value.
 * Collected during job execution so that individual failures don't abort the entire job.
 */
public class RuleApplicationError {

    private String ruleId;
    private String ruleName;
    private String fieldName;
    private String originalValue;   // the value that failed to transform
    private String reason;          // e.g. "Unparseable date 'not-a-date' with format MM/dd/yyyy"
    private int    recordIndex;     // 0-based index of the record in the dataset

    public RuleApplicationError() {}

    private RuleApplicationError(Builder builder) {
        this.ruleId = builder.ruleId;
        this.ruleName = builder.ruleName;
        this.fieldName = builder.fieldName;
        this.originalValue = builder.originalValue;
        this.reason = builder.reason;
        this.recordIndex = builder.recordIndex;
    }

    public static Builder builder() {
        return new Builder();
    }

    // ── Getters & Setters ────────────────────────────────────────

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }

    public String getOriginalValue() { return originalValue; }
    public void setOriginalValue(String originalValue) { this.originalValue = originalValue; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public int getRecordIndex() { return recordIndex; }
    public void setRecordIndex(int recordIndex) { this.recordIndex = recordIndex; }

    // ── Builder ──────────────────────────────────────────────────

    public static class Builder {
        private String ruleId;
        private String ruleName;
        private String fieldName;
        private String originalValue;
        private String reason;
        private int recordIndex;

        public Builder ruleId(String ruleId) { this.ruleId = ruleId; return this; }
        public Builder ruleName(String ruleName) { this.ruleName = ruleName; return this; }
        public Builder fieldName(String fieldName) { this.fieldName = fieldName; return this; }
        public Builder originalValue(String originalValue) { this.originalValue = originalValue; return this; }
        public Builder reason(String reason) { this.reason = reason; return this; }
        public Builder recordIndex(int recordIndex) { this.recordIndex = recordIndex; return this; }

        public RuleApplicationError build() {
            return new RuleApplicationError(this);
        }
    }
}

