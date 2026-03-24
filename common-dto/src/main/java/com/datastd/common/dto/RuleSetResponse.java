package com.datastd.common.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Shared DTO representing a rule set (ordered collection of rules).
 * Returned by rule-engine-service and consumed by standardization-service
 * via its Feign client.
 */
public class RuleSetResponse {

    private UUID id;
    private String name;
    private String description;
    private List<UUID> ruleIds;
    private List<RuleResponse> rules;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public RuleSetResponse() {}

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

    public List<RuleResponse> getRules() {
        return rules;
    }

    public void setRules(List<RuleResponse> rules) {
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

