package com.datastd.standardization.service.rules.impl;

import com.datastd.standardization.service.rules.RuleApplier;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Applies regex replacement on the value.
 * Config: {"pattern": "regex_pattern", "replacement": "replacement_text"}
 */
public class RegexApplier implements RuleApplier {
    @Override
    public String apply(String value, Map<String, Object> config) {
        if (value == null || config == null) return value;

        String pattern = (String) config.get("pattern");
        String replacement = (String) config.get("replacement");
        if (replacement == null) replacement = "";

        if (pattern == null || pattern.isEmpty()) return value;

        try {
            return Pattern.compile(pattern).matcher(value).replaceAll(replacement);
        } catch (Exception e) {
            return value;
        }
    }
}

