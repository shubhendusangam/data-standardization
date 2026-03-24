package com.datastd.standardization.service.rules;

import com.datastd.common.dto.RuleResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Applies an ordered list of rules to a single data record.
 * Shared between {@code StandardizationServiceImpl} (preview) and
 * {@code AsyncJobProcessor} (async job execution) to prevent code divergence.
 */
@Component
public class RuleExecutionEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleExecutionEngine.class);

    private final RuleApplierFactory ruleApplierFactory;
    private final ObjectMapper objectMapper;

    public RuleExecutionEngine(RuleApplierFactory ruleApplierFactory, ObjectMapper objectMapper) {
        this.ruleApplierFactory = ruleApplierFactory;
        this.objectMapper = objectMapper;
    }

    /**
     * Applies all active rules (in order) to a single record, returning the transformed record.
     * Inactive rules are skipped. Individual rule failures are logged and skipped (non-fatal).
     */
    public Map<String, Object> applyRulesToRecord(Map<String, Object> record,
                                                   List<RuleResponse> rules) {
        Map<String, Object> result = new LinkedHashMap<>(record);

        for (RuleResponse rule : rules) {
            if (!rule.isActive()) continue;

            String fieldName = rule.getFieldName();
            Object fieldValue = result.get(fieldName);

            if (fieldValue == null && !"DEFAULT_VALUE".equals(rule.getRuleType())) {
                continue;
            }

            String stringValue = fieldValue != null ? String.valueOf(fieldValue) : null;

            try {
                Map<String, Object> config = null;
                if (rule.getRuleConfig() != null && !rule.getRuleConfig().isEmpty()) {
                    config = objectMapper.readValue(rule.getRuleConfig(),
                            new TypeReference<Map<String, Object>>() {});
                }

                RuleApplier applier = ruleApplierFactory.getApplier(rule.getRuleType());
                String transformed = applier.apply(stringValue, config);
                result.put(fieldName, transformed);
            } catch (Exception e) {
                log.warn("Failed to apply rule '{}' on field '{}': {}",
                        rule.getName(), fieldName, e.getMessage());
            }
        }

        return result;
    }
}

