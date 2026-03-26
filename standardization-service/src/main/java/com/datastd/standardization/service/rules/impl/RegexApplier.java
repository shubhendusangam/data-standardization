package com.datastd.standardization.service.rules.impl;

import com.datastd.standardization.exception.RuleApplicationException;
import com.datastd.standardization.service.rules.RuleApplier;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Applies regex replacement on the value.
 * Config: {"pattern": "regex_pattern", "replacement": "replacement_text"}
 */
@Component
public class RegexApplier implements RuleApplier {

    @Override
    public String getType() {
        return "REGEX";
    }

    @Override
    public String apply(String value, Map<String, Object> config) {
        if (value == null || config == null) return value;

        String pattern = (String) config.get("pattern");
        String replacement = (String) config.get("replacement");
        if (replacement == null) replacement = "";

        if (pattern == null || pattern.isEmpty()) return value;

        try {
            return Pattern.compile(pattern).matcher(value).replaceAll(replacement);
        } catch (PatternSyntaxException e) {
            throw new RuleApplicationException(
                    "Invalid regex pattern '" + pattern + "': " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuleApplicationException(
                    "Regex replacement failed for value '" + value + "': " + e.getMessage(), e);
        }
    }
}

