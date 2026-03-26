package com.datastd.standardization.service.rules;

import com.datastd.common.dto.RuleApplicationError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Wraps the result of applying rules to a single record.
 * Contains the (possibly partially) transformed record and any per-field errors
 * that occurred during rule application.
 */
public class RuleApplicationResult {

    private final Map<String, Object> record;
    private final List<RuleApplicationError> errors;

    public RuleApplicationResult(Map<String, Object> record, List<RuleApplicationError> errors) {
        this.record = record;
        this.errors = errors != null ? errors : new ArrayList<>();
    }

    public Map<String, Object> getRecord() {
        return record;
    }

    public List<RuleApplicationError> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}

