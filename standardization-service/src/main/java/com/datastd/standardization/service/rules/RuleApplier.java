package com.datastd.standardization.service.rules;

import java.util.Map;

/**
 * Strategy interface for rule application.
 * Each implementation handles a specific rule type (TRIM, UPPERCASE, etc.).
 */
public interface RuleApplier {

    /**
     * Apply the rule to the given value.
     *
     * @param value  the field value to transform
     * @param config rule configuration parameters
     * @return the transformed value
     */
    String apply(String value, Map<String, Object> config);

    /**
     * Returns the rule type this applier handles (e.g., "TRIM", "UPPERCASE").
     * Used for auto-discovery by {@link RuleApplierFactory}.
     */
    String getType();
}

