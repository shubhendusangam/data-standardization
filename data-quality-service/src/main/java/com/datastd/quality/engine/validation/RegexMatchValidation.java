package com.datastd.quality.engine.validation;

import com.datastd.common.dto.ValidationRuleResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class RegexMatchValidation extends AbstractValidationStrategy {

    @Override
    public void evaluate(ValidationRuleResult result, String column,
                         List<Map<String, Object>> records, Map<String, Object> params) {
        int total = records.size();
        String patternStr = (String) params.getOrDefault("pattern", ".*");
        double maxFailRate = getDouble(params, "maxFailRatePct", 0.0);
        Pattern pattern = Pattern.compile(patternStr);

        long failCount = records.stream().filter(r -> {
            Object val = r.get(column);
            if (val == null) return true;
            return !pattern.matcher(val.toString()).matches();
        }).count();

        double failRate = round((double) failCount / total * 100);
        result.setFailCount((int) failCount);
        result.setFailRatePct(failRate);
        result.setPassed(failRate <= maxFailRate);
        result.setMessage(String.format("%s: %.1f%% of values fail regex match (threshold: %.1f%%)", column, failRate, maxFailRate));
    }
}

