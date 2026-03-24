package com.datastd.standardization.service.rules.impl;

import com.datastd.standardization.service.rules.RuleApplier;

import java.util.Map;

public class UppercaseApplier implements RuleApplier {
    @Override
    public String apply(String value, Map<String, Object> config) {
        if (value == null) return null;
        return value.toUpperCase();
    }
}

