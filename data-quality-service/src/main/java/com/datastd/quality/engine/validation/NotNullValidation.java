package com.datastd.quality.engine.validation;

import com.datastd.common.dto.ValidationRuleResult;
import com.datastd.quality.entity.ValidationType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class NotNullValidation extends AbstractValidationStrategy {

    @Override
    public void evaluate(ValidationRuleResult result, String column,
                         List<Map<String, Object>> records, Map<String, Object> params) {
        int total = records.size();
        long nullCount = records.stream().filter(r -> r.get(column) == null).count();
        double nullRate = round((double) nullCount / total * 100);
        double maxRate = getDouble(params, "maxNullRatePct", 0.0);

        result.setFailCount((int) nullCount);
        result.setFailRatePct(nullRate);
        result.setPassed(nullRate <= maxRate);
        result.setMessage(String.format("%s: %.1f%% null values (threshold: %.1f%%)", column, nullRate, maxRate));
    }
}

