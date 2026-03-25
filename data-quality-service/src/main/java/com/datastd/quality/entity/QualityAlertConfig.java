package com.datastd.quality.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Webhook alert configuration. Triggers outbound POST on quality events.
 */
@Entity
@Table(name = "quality_alert_configs")
public class QualityAlertConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String webhookUrl;

    /** Comma-separated OverallStatus values, e.g. "FAIL,WARN" */
    @Column(nullable = false)
    private String triggerOnStatus;

    /** Alert if qualityScore drops below this threshold (nullable = no score-based trigger) */
    private Integer triggerOnScoreBelow;

    @Column(nullable = false)
    private boolean active = true;

    /** HMAC-SHA256 secret for signing outbound webhook payloads */
    @Column(nullable = false)
    private String secret;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public String getTriggerOnStatus() { return triggerOnStatus; }
    public void setTriggerOnStatus(String triggerOnStatus) { this.triggerOnStatus = triggerOnStatus; }
    public Integer getTriggerOnScoreBelow() { return triggerOnScoreBelow; }
    public void setTriggerOnScoreBelow(Integer triggerOnScoreBelow) { this.triggerOnScoreBelow = triggerOnScoreBelow; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}

