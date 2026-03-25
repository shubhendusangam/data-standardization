package com.datastd.quality.dto;

import com.datastd.quality.entity.Severity;
import com.datastd.quality.entity.ValidationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for creating/updating a ValidationRule.
 */
public class ValidationRuleRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "columnName is required")
    private String columnName;

    @NotNull(message = "validationType is required")
    private ValidationType validationType;

    private String params;

    @NotNull(message = "severity is required")
    private Severity severity;

    private boolean active = true;

    // Getters and Setters

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public ValidationType getValidationType() {
        return validationType;
    }

    public void setValidationType(ValidationType validationType) {
        this.validationType = validationType;
    }

    public String getParams() {
        return params;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

