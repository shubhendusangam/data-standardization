package com.datastd.standardization.service.rules.impl;

import com.datastd.standardization.service.rules.RuleApplier;

import java.util.Map;

/**
 * Maps specific values to standardized values.
 * Config: {"mappings": {"M": "Male", "F": "Female", "m": "Male", "f": "Female"}}
 */
@SuppressWarnings("unchecked")
public class MapValuesApplier implements RuleApplier {
    @Override
    public String apply(String value, Map<String, Object> config) {
        if (value == null || config == null) return value;

        Object mappingsObj = config.get("mappings");
        if (mappingsObj instanceof Map) {
            Map<String, String> mappings = (Map<String, String>) mappingsObj;
            return mappings.getOrDefault(value, value);
        }
        return value;
    }
}

