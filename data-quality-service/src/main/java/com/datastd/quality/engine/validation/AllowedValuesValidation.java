package com.datastd.quality.engine.validation;

import com.datastd.common.dto.ValidationRuleResult;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AllowedValuesValidation extends AbstractValidationStrategy {

    @SuppressWarnings("unchecked")
    @Override
    public void evaluate(ValidationRuleResult result, String column,
                         List<Map<String, Object>> records, Map<String, Object> params) {
        int total = records.size();
        List<String> allowed = (List<String>) params.getOrDefault("values", List.of());
        Set<String> allowedSet = new HashSet<>(allowed);
        double maxFailRate = getDouble(params, "maxFailRatePct", 0.0);

        long failCount = records.stream().filter(r -> {
            Object val = r.get(column);
            if (val == null) return true;
            return !allowedSet.contains(val.toString());
        }).count();

        double failRate = round((double) failCount / total * 100);
        result.setFailCount((int) failCount);
        result.setFailRatePct(failRate);
        result.setPassed(failRate <= maxFailRate);
        result.setMessage(String.format("%s: %.1f%% of values not in allowed set (threshold: %.1f%%)", column, failRate, maxFailRate));
    }
}

