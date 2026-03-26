package com.datastd.quality.engine.validation;

import com.datastd.common.dto.ValidationRuleResult;

import java.util.List;
import java.util.Map;

/**
 * Strategy interface for validation type implementations.
 * Each validation type (NOT_NULL, REGEX_MATCH, etc.) implements this interface.
 * New validation types can be added by creating a new implementation without
 * modifying the ValidationEngine.
 */
public interface ValidationStrategy {

    /**
     * Evaluate the validation rule against the records for the given column.
     *
     * @param result     the result object to populate with pass/fail info
     * @param columnName the column to validate
     * @param records    the dataset records
     * @param params     parsed rule parameters
     */
    void evaluate(ValidationRuleResult result, String columnName,
                  List<Map<String, Object>> records, Map<String, Object> params);
}

