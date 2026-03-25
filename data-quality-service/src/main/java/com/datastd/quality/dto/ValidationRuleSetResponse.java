package com.datastd.quality.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response body for a ValidationRuleSet.
 */
public class ValidationRuleSetResponse {

    private UUID id;
    private String name;
    private String description;
    private List<UUID> ruleIds;
    private List<ValidationRuleResponse> rules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ValidationRuleSetResponse() {}

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public List<ValidationRuleResponse> getRules() {
        return rules;
    }

    public void setRules(List<ValidationRuleResponse> rules) {
        this.rules = rules;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

