package com.datastd.standardization.service.rules.impl;

import com.datastd.standardization.service.rules.RuleApplier;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Applies find-and-replace on the value.
 * Config: {"find": "old_text", "replace": "new_text"}
 */
@Component
public class ReplaceApplier implements RuleApplier {

    @Override
    public String getType() {
        return "REPLACE";
    }

    @Override
    public String apply(String value, Map<String, Object> config) {
        if (value == null || config == null) return value;

        String find = (String) config.get("find");
        String replace = (String) config.get("replace");
        if (replace == null) replace = "";

        if (find == null || find.isEmpty()) return value;
        return value.replace(find, replace);
    }
}

