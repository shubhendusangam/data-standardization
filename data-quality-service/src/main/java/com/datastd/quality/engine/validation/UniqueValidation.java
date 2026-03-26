package com.datastd.quality.engine.validation;

import com.datastd.common.dto.ValidationRuleResult;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class UniqueValidation extends AbstractValidationStrategy {

    @Override
    public void evaluate(ValidationRuleResult result, String column,
                         List<Map<String, Object>> records, Map<String, Object> params) {
        int total = records.size();
        double maxDupRate = getDouble(params, "maxDuplicateRatePct", 0.0);

        Map<String, Integer> valueCounts = new HashMap<>();
        for (Map<String, Object> record : records) {
            Object val = record.get(column);
            String key = val == null ? "__NULL__" : val.toString();
            valueCounts.merge(key, 1, Integer::sum);
        }

        long duplicateCount = 0;
        for (int count : valueCounts.values()) {
            if (count > 1) duplicateCount += (count - 1);
        }

        double dupRate = total == 0 ? 0 : round((double) duplicateCount / total * 100);
        result.setFailCount((int) duplicateCount);
        result.setFailRatePct(dupRate);
        result.setPassed(dupRate <= maxDupRate);
        result.setMessage(String.format("%s: %.1f%% duplicate values (threshold: %.1f%%)", column, dupRate, maxDupRate));
    }
}

