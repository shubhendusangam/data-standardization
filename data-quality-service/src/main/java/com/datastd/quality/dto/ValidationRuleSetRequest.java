package com.datastd.quality.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Request body for creating a ValidationRuleSet.
 */
public class ValidationRuleSetRequest {

    @NotBlank(message = "name is required")
    private String name;

    private String description;

    @NotEmpty(message = "ruleIds must not be empty")
    private List<UUID> ruleIds;

    // Getters and Setters

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

