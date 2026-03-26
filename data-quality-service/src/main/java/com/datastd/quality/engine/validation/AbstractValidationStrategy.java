package com.datastd.quality.engine.validation;

import com.datastd.common.dto.ValidationRuleResult;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Base class for validation strategies providing common utility methods.
 */
public abstract class AbstractValidationStrategy implements ValidationStrategy {

    protected double round(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    protected double getDouble(Map<String, Object> params, String key, double defaultValue) {
        Object val = params.get(key);
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected int getInt(Map<String, Object> params, String key, int defaultValue) {
        Object val = params.get(key);
        if (val == null) return defaultValue;
        if (val instanceof Number) return ((Number) val).intValue();
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    protected void setResult(ValidationRuleResult result, long failCount, int total,
                             double maxFailRate, String message) {
        double failRate = total == 0 ? 0 : round((double) failCount / total * 100);
        result.setFailCount((int) failCount);
        result.setFailRatePct(failRate);
        result.setPassed(failRate <= maxFailRate);
        result.setMessage(message);
    }
}

