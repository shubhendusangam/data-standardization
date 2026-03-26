package com.datastd.standardization.service.rules;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Factory that resolves {@link RuleApplier} by rule type using Spring auto-discovery.
 * All {@code @Component} beans implementing {@link RuleApplier} are automatically
 * registered — adding a new rule type only requires creating a new class.
 */
@Component
public class RuleApplierFactory {

    private final Map<String, RuleApplier> appliers = new HashMap<>();

    public RuleApplierFactory(List<RuleApplier> ruleAppliers) {
        for (RuleApplier applier : ruleAppliers) {
            appliers.put(applier.getType(), applier);
        }
    }

    public RuleApplier getApplier(String ruleType) {
        RuleApplier applier = appliers.get(ruleType);
        if (applier == null) {
            throw new IllegalArgumentException("Unknown rule type: " + ruleType);
        }
        return applier;
    }
}

