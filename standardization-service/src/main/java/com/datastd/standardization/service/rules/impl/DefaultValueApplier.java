package com.datastd.standardization.service.rules.impl;

import com.datastd.standardization.service.rules.RuleApplier;

import java.util.Map;

/**
 * Sets a default value when the field is null or empty.
 * Config: {"defaultValue": "N/A"}
 */
public class DefaultValueApplier implements RuleApplier {
    @Override
    public String apply(String value, Map<String, Object> config) {
        if (config == null) return value;

        String defaultValue = (String) config.get("defaultValue");

        if (value == null || value.trim().isEmpty()) {
            return defaultValue != null ? defaultValue : value;
        }
        return value;
    }
}

