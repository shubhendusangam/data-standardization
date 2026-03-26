package com.datastd.quality.engine.validation;

import com.datastd.common.dto.ValidationRuleResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Placeholder for CUSTOM_SQL validation type (reserved for future use).
 */
@Component
public class CustomSqlValidation extends AbstractValidationStrategy {

    @Override
    public void evaluate(ValidationRuleResult result, String column,
                         List<Map<String, Object>> records, Map<String, Object> params) {
        result.setPassed(true);
        result.setFailRatePct(0);
        result.setFailCount(0);
        result.setMessage(column + ": CUSTOM_SQL not yet implemented");
    }
}

