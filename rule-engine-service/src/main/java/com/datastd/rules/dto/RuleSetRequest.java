package com.datastd.rules.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public class RuleSetRequest {

    @NotBlank(message = "Rule set name is required")
    private String name;

    private String description;

    @NotEmpty(message = "Rule IDs list cannot be empty")
    private List<UUID> ruleIds;

    public RuleSetRequest() {}

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

    public List<UUID> getRuleIds() {
        return ruleIds;
    }

    public void setRuleIds(List<UUID> ruleIds) {
        this.ruleIds = ruleIds;
    }
}

