package com.datastd.standardization.service.rules;

import com.datastd.common.dto.RuleApplicationError;
import com.datastd.common.dto.RuleResponse;
import com.datastd.standardization.exception.RuleApplicationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
     * Applies all active rules (in order) to a single record, returning the transformed record
     * along with any per-field errors.
     * Inactive rules are skipped. Individual rule failures are captured in the result (non-fatal):
     * the record retains its original value for the failed field and processing continues.
     */
    public RuleApplicationResult applyRulesToRecord(Map<String, Object> record,
                                                     List<RuleResponse> rules) {
        Map<String, Object> result = new LinkedHashMap<>(record);
        List<RuleApplicationError> errors = new ArrayList<>();

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
            } catch (RuleApplicationException e) {
                // Keep original value — do not overwrite with null
                log.debug("Rule application error: rule='{}', field='{}', value='{}': {}",
                        rule.getName(), fieldName, stringValue, e.getMessage());
                errors.add(RuleApplicationError.builder()
                        .ruleId(rule.getId() != null ? rule.getId().toString() : null)
                        .ruleName(rule.getName())
                        .fieldName(fieldName)
                        .originalValue(stringValue)
                        .reason(e.getMessage())
                        .build());
            } catch (Exception e) {
                log.warn("Failed to apply rule '{}' on field '{}': {}",
                        rule.getName(), fieldName, e.getMessage());
                errors.add(RuleApplicationError.builder()
                        .ruleId(rule.getId() != null ? rule.getId().toString() : null)
                        .ruleName(rule.getName())
                        .fieldName(fieldName)
                        .originalValue(stringValue)
                        .reason(e.getMessage())
                        .build());
            }
        }

        return new RuleApplicationResult(result, errors);
    }
}

