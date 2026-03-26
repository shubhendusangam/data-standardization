package com.datastd.quality.engine.validation;

import com.datastd.common.dto.ValidationRuleResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class NumericRangeValidation extends AbstractValidationStrategy {

    @Override
    public void evaluate(ValidationRuleResult result, String column,
                         List<Map<String, Object>> records, Map<String, Object> params) {
        int total = records.size();
        double min = getDouble(params, "min", Double.NEGATIVE_INFINITY);
        double max = getDouble(params, "max", Double.POSITIVE_INFINITY);
        double maxFailRate = getDouble(params, "maxFailRatePct", 0.0);

        long failCount = records.stream().filter(r -> {
            Object val = r.get(column);
            if (val == null) return true;
            try {
                double num = Double.parseDouble(val.toString());
                return num < min || num > max;
            } catch (NumberFormatException e) {
                return true;
            }
        }).count();

        double failRate = round((double) failCount / total * 100);
        result.setFailCount((int) failCount);
        result.setFailRatePct(failRate);
        result.setPassed(failRate <= maxFailRate);
        result.setMessage(String.format("%s: %.1f%% of values outside range [%.1f, %.1f] (threshold: %.1f%%)",
                column, failRate, min, max, maxFailRate));
    }
}

