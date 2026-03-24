package com.datastd.rules.dto;

import com.datastd.rules.entity.StandardizationRule;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RuleRequest {

    @NotBlank(message = "Rule name is required")
    private String name;

    private String description;

    @NotBlank(message = "Field name is required")
    private String fieldName;

    @NotNull(message = "Rule type is required")
    private StandardizationRule.RuleType ruleType;

    private String ruleConfig;

    @Min(value = 0, message = "Priority must be >= 0")
    private int priority = 0;

    private boolean active = true;

    public RuleRequest() {}

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public StandardizationRule.RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(StandardizationRule.RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public String getRuleConfig() {
        return ruleConfig;
    }

    public void setRuleConfig(String ruleConfig) {
        this.ruleConfig = ruleConfig;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

