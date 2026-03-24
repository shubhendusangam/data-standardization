package com.datastd.standardization.service.rules;

import com.datastd.standardization.service.rules.impl.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class RuleApplierFactory {

    private final Map<String, RuleApplier> appliers = new HashMap<>();

    public RuleApplierFactory() {
        appliers.put("TRIM", new TrimApplier());
        appliers.put("UPPERCASE", new UppercaseApplier());
        appliers.put("LOWERCASE", new LowercaseApplier());
        appliers.put("REPLACE", new ReplaceApplier());
        appliers.put("MAP_VALUES", new MapValuesApplier());
        appliers.put("REGEX", new RegexApplier());
        appliers.put("DEFAULT_VALUE", new DefaultValueApplier());
        appliers.put("DATE_FORMAT", new DateFormatApplier());
    }

    public RuleApplier getApplier(String ruleType) {
        RuleApplier applier = appliers.get(ruleType);
        if (applier == null) {
            throw new IllegalArgumentException("Unknown rule type: " + ruleType);
        }
        return applier;
    }
}

