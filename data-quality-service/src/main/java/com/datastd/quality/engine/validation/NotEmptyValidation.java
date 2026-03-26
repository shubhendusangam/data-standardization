package com.datastd.quality.engine.validation;

import com.datastd.common.dto.ValidationRuleResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class NotEmptyValidation extends AbstractValidationStrategy {

    @Override
    public void evaluate(ValidationRuleResult result, String column,
                         List<Map<String, Object>> records, Map<String, Object> params) {
        int total = records.size();
        long emptyCount = records.stream().filter(r -> {
            Object val = r.get(column);
            return val == null || val.toString().isBlank();
        }).count();
        double emptyRate = round((double) emptyCount / total * 100);
        double maxRate = getDouble(params, "maxNullRatePct", 0.0);

        result.setFailCount((int) emptyCount);
        result.setFailRatePct(emptyRate);
        result.setPassed(emptyRate <= maxRate);
        result.setMessage(String.format("%s: %.1f%% blank/null values (threshold: %.1f%%)", column, emptyRate, maxRate));
    }
}

