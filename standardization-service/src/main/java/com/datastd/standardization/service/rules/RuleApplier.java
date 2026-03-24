package com.datastd.standardization.service.rules;

import java.util.Map;

public interface RuleApplier {
    String apply(String value, Map<String, Object> config);
}

