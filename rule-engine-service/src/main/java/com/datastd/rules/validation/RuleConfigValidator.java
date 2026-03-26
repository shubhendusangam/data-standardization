package com.datastd.rules.validation;

import com.datastd.rules.entity.StandardizationRule.RuleType;
import com.datastd.rules.exception.RuleConfigException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Validates that ruleConfig JSON contains the required keys for the given ruleType.
 */
@Component
public class RuleConfigValidator {

    private final ObjectMapper objectMapper;

    public RuleConfigValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Validates the ruleConfig string against the schema expected by the given ruleType.
     *
     * @param ruleType   the rule type enum
     * @param ruleConfig the raw JSON string (may be null or blank for no-config types)
     * @throws RuleConfigException if ruleConfig is invalid
     */
    public void validate(RuleType ruleType, String ruleConfig) {
        String typeName = ruleType.name();

        switch (ruleType) {
            case TRIM, UPPERCASE, LOWERCASE -> {
                // No config required — null, blank, or empty object are all fine
                return;
            }
            default -> {
                // All other types require a non-empty JSON config
            }
        }

        JsonNode parsed = parseJson(ruleConfig, typeName);

        switch (ruleType) {
            case REPLACE -> requireKeys(parsed, typeName, "find", "replace");
            case MAP_VALUES -> {
                requireKeys(parsed, typeName, "mappings");
                if (!parsed.get("mappings").isObject()) {
                    throw new RuleConfigException(
                            "'mappings' must be a JSON object {key: value, ...}", typeName);
                }
            }
            case REGEX -> requireKeys(parsed, typeName, "pattern", "replacement");
            case DEFAULT_VALUE -> requireKeys(parsed, typeName, "defaultValue");
            case DATE_FORMAT -> requireKeys(parsed, typeName, "sourceFormat", "targetFormat");
            default -> throw new RuleConfigException("Unknown ruleType: " + typeName, typeName);
        }
    }

    private JsonNode parseJson(String ruleConfig, String ruleType) {
        if (ruleConfig == null || ruleConfig.isBlank()) {
            throw new RuleConfigException(
                    "ruleConfig is required for ruleType " + ruleType, ruleType);
        }
        try {
            JsonNode node = objectMapper.readTree(ruleConfig);
            if (!node.isObject()) {
                throw new RuleConfigException(
                        "ruleConfig must be a JSON object", ruleType);
            }
            return node;
        } catch (JsonProcessingException e) {
            throw new RuleConfigException(
                    "ruleConfig contains invalid JSON: " + e.getOriginalMessage(), ruleType);
        }
    }

    private void requireKeys(JsonNode node, String ruleType, String... keys) {
        for (String key : keys) {
            if (!node.has(key) || node.get(key).isNull()) {
                throw new RuleConfigException(
                        "ruleConfig missing required key '" + key + "' for ruleType " + ruleType,
                        ruleType);
            }
        }
    }
}

