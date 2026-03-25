package com.datastd.quality.dto;

import java.util.List;
import java.util.UUID;

/**
 * Request/response for alert webhook configuration.
 */
public class AlertConfigRequest {

    private String name;
    private String webhookUrl;
    private List<String> triggerOnStatus; // "FAIL", "WARN"
    private Integer triggerOnScoreBelow;
    private boolean active = true;
    private String secret;

    public AlertConfigRequest() {}

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
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
}

