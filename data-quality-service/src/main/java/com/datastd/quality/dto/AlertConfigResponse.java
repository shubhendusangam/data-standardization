package com.datastd.quality.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for alert webhook configuration.
 */
public class AlertConfigResponse {

    private UUID id;
    private String name;
    private String webhookUrl;
    private List<String> triggerOnStatus;
    private Integer triggerOnScoreBelow;
    private boolean active;
    private LocalDateTime createdAt;

    public AlertConfigResponse() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public List<String> getTriggerOnStatus() { return triggerOnStatus; }
    public void setTriggerOnStatus(List<String> triggerOnStatus) { this.triggerOnStatus = triggerOnStatus; }
    public Integer getTriggerOnScoreBelow() { return triggerOnScoreBelow; }
    public void setTriggerOnScoreBelow(Integer triggerOnScoreBelow) { this.triggerOnScoreBelow = triggerOnScoreBelow; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

