package com.datastd.quality.engine.validation;

import com.datastd.common.dto.ValidationRuleResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MinLengthValidation extends AbstractValidationStrategy {

    @Override
    public void evaluate(ValidationRuleResult result, String column,
                         List<Map<String, Object>> records, Map<String, Object> params) {
        int total = records.size();
        int minLen = getInt(params, "length", 0);

        long failCount = records.stream().filter(r -> {
            Object val = r.get(column);
            if (val == null) return true;
            return val.toString().length() < minLen;
        }).count();

        double failRate = round((double) failCount / total * 100);
        result.setFailCount((int) failCount);
        result.setFailRatePct(failRate);
        result.setPassed(failCount == 0);
        result.setMessage(String.format("%s: %d values shorter than %d chars", column, failCount, minLen));
    }
}

