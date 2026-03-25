package com.datastd.quality.dto;

/**
 * A suggested validation rule returned by POST /api/quality/rules/suggest.
 * The client can accept suggestions and POST them to /api/quality/rules.
 */
public class SuggestedRule {

    private String validationType;
    private String columnName;
    private String params;
    private String severity;
    private String rationale;
    private String confidence; // HIGH, MEDIUM, LOW

    public SuggestedRule() {}

    public SuggestedRule(String validationType, String columnName, String params,
                         String severity, String rationale, String confidence) {
        this.validationType = validationType;
        this.columnName = columnName;
        this.params = params;
        this.severity = severity;
        this.rationale = rationale;
        this.confidence = confidence;
    }

    public String getValidationType() { return validationType; }
    public void setValidationType(String validationType) { this.validationType = validationType; }

    public String getColumnName() { return columnName; }
    public void setColumnName(String columnName) { this.columnName = columnName; }

    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }

    public String getConfidence() { return confidence; }
    public void setConfidence(String confidence) { this.confidence = confidence; }
}

