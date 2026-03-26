package com.datastd.standardization.service.rules.impl;

import com.datastd.standardization.service.rules.RuleApplier;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TrimApplier implements RuleApplier {

    @Override
    public String getType() {
        return "TRIM";
    }

    @Override
    public String apply(String value, Map<String, Object> config) {
        if (value == null) return null;
        return value.trim();
    }
}

