package com.datastd.quality.service;

import com.datastd.common.dto.QualityReport;
import com.datastd.common.dto.ValidationRuleResult;
import com.datastd.quality.entity.QualityAlertConfig;
import com.datastd.quality.repository.QualityAlertConfigRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Async service that delivers webhook alerts after quality validation.
 * Fire-and-forget: alert failure never blocks the validation response.
 */
@Service
public class QualityAlertService {

    private static final Logger log = LoggerFactory.getLogger(QualityAlertService.class);
    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    private final QualityAlertConfigRepository alertConfigRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public QualityAlertService(QualityAlertConfigRepository alertConfigRepository,
                               ObjectMapper objectMapper,
                               RestTemplate restTemplate) {
        this.alertConfigRepository = alertConfigRepository;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    /**
     * Check all active alert configs and fire webhooks if conditions match.
     * Called after every POST /api/quality/validate.
     */
    @Async("alertTaskExecutor")
    public void evaluateAndFireAlerts(QualityReport report) {
        try {
            List<QualityAlertConfig> configs = alertConfigRepository.findByActive(true);
            for (QualityAlertConfig config : configs) {
                if (shouldTrigger(config, report)) {
                    fireWebhook(config, report);
                }
            }
        } catch (Exception e) {
            log.error("Error evaluating alerts for reportId={}: {}", report.getReportId(), e.getMessage());
        }
    }

    private boolean shouldTrigger(QualityAlertConfig config, QualityReport report) {
        // Check status trigger
        String statusStr = config.getTriggerOnStatus();
        if (statusStr != null && !statusStr.isBlank()) {
            Set<String> statuses = Set.of(statusStr.split(","));
            if (statuses.contains(report.getOverallStatus().name())) {
                return true;
            }
        }
        // Check score threshold trigger
        if (config.getTriggerOnScoreBelow() != null
                && report.getQualityScore() < config.getTriggerOnScoreBelow()) {
            return true;
        }
        return false;
    }

    private void fireWebhook(QualityAlertConfig config, QualityReport report) {
        try {
            Map<String, Object> payload = buildPayload(config, report);
            String body = objectMapper.writeValueAsString(payload);
            String signature = computeHmac(body, config.getSecret());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Quality-Signature", signature);

            HttpEntity<String> request = new HttpEntity<>(body, headers);

            for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                try {
                    ResponseEntity<String> response = restTemplate.postForEntity(
                            config.getWebhookUrl(), request, String.class);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        log.info("Alert delivered: alertId={}, url={}, status={}",
                                config.getId(), config.getWebhookUrl(), response.getStatusCode());
                        return;
                    }
                    log.warn("Alert delivery non-2xx: alertId={}, url={}, status={}, attempt={}/{}",
                            config.getId(), config.getWebhookUrl(), response.getStatusCode(), attempt, MAX_RETRIES);
                } catch (Exception e) {
                    log.warn("Alert delivery error: alertId={}, url={}, attempt={}/{}, error={}",
                            config.getId(), config.getWebhookUrl(), attempt, MAX_RETRIES, e.getMessage());
                }
                if (attempt < MAX_RETRIES) {
                    long delay = (long) (INITIAL_RETRY_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, attempt - 1));
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.warn("Alert retry interrupted: alertId={}", config.getId());
                        return;
                    }
                }
            }
            log.warn("Alert delivery failed: alertId={}, url={}, exhausted all {} retries",
                    config.getId(), config.getWebhookUrl(), MAX_RETRIES);

        } catch (Exception e) {
            log.error("Failed to build/send alert: alertId={}, error={}", config.getId(), e.getMessage());
        }
    }

    private Map<String, Object> buildPayload(QualityAlertConfig config, QualityReport report) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", "QUALITY_ALERT");
        payload.put("datasetId", report.getDatasetId());
        payload.put("overallStatus", report.getOverallStatus().name());
        payload.put("qualityScore", report.getQualityScore());

        List<Map<String, String>> failedRules = Collections.emptyList();
        if (report.getRuleResults() != null) {
            failedRules = report.getRuleResults().stream()
                    .filter(r -> !r.isPassed())
                    .map(r -> {
                        Map<String, String> m = new LinkedHashMap<>();
                        m.put("ruleName", r.getRuleName());
                        m.put("columnName", r.getColumnName());
                        m.put("message", r.getMessage());
                        return m;
                    })
                    .collect(Collectors.toList());
        }
        payload.put("failedRules", failedRules);

        if (report.getReportId() != null) {
            payload.put("reportUrl", "http://localhost:8080/api/quality/reports/" + report.getReportId() + "/full");
        }
        payload.put("timestamp", Instant.now().toString());
        return payload;
    }

    /**
     * Compute HMAC-SHA256 hex signature.
     */
    public static String computeHmac(String data, String secret) {
        if (secret == null || secret.isEmpty()) {
            return "";
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256", e);
        }
    }
}

