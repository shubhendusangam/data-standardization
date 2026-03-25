package com.datastd.quality.mapper;

import com.datastd.quality.dto.ValidationRuleResponse;
import com.datastd.quality.dto.ValidationRuleSetResponse;
import com.datastd.quality.entity.ValidationRule;
import com.datastd.quality.entity.ValidationRuleSet;

import java.util.List;

/**
 * Maps quality entities to response DTOs.
 */
public final class QualityMapper {

    private QualityMapper() {}

    public static ValidationRuleResponse toResponse(ValidationRule rule) {
        ValidationRuleResponse dto = new ValidationRuleResponse();
        dto.setId(rule.getId());
        dto.setName(rule.getName());
        dto.setColumnName(rule.getColumnName());
        dto.setValidationType(rule.getValidationType().name());
        dto.setParams(rule.getParams());
        dto.setSeverity(rule.getSeverity().name());
        dto.setActive(rule.isActive());
        dto.setCreatedAt(rule.getCreatedAt());
        dto.setUpdatedAt(rule.getUpdatedAt());
        return dto;
    }

    public static ValidationRuleSetResponse toResponse(ValidationRuleSet ruleSet) {
        ValidationRuleSetResponse dto = new ValidationRuleSetResponse();
        dto.setId(ruleSet.getId());
        dto.setName(ruleSet.getName());
        dto.setDescription(ruleSet.getDescription());
        dto.setRuleIds(ruleSet.getRuleIds());
        dto.setCreatedAt(ruleSet.getCreatedAt());
        dto.setUpdatedAt(ruleSet.getUpdatedAt());
        return dto;
    }

    public static ValidationRuleSetResponse toResponse(ValidationRuleSet ruleSet, List<ValidationRuleResponse> rules) {
        ValidationRuleSetResponse dto = toResponse(ruleSet);
        dto.setRules(rules);
        return dto;
    }
}

