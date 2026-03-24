package com.datastd.rules.mapper;

import com.datastd.common.dto.RuleResponse;
import com.datastd.common.dto.RuleSetResponse;
import com.datastd.rules.entity.RuleSet;
import com.datastd.rules.entity.StandardizationRule;

import java.util.List;

/**
 * Maps rule-engine entities to the shared DTOs from common-dto.
 * Keeps fromEntity logic local to this service (common-dto has no entity dependency).
 */
public final class RuleMapper {

    private RuleMapper() {}

    public static RuleResponse toResponse(StandardizationRule rule) {
        RuleResponse dto = new RuleResponse();
        dto.setId(rule.getId());
        dto.setName(rule.getName());
        dto.setDescription(rule.getDescription());
        dto.setFieldName(rule.getFieldName());
        dto.setRuleType(rule.getRuleType().name());
        dto.setRuleConfig(rule.getRuleConfig());
        dto.setPriority(rule.getPriority());
        dto.setActive(rule.isActive());
        dto.setCreatedAt(rule.getCreatedAt());
        dto.setUpdatedAt(rule.getUpdatedAt());
        return dto;
    }

    public static RuleSetResponse toResponse(RuleSet ruleSet) {
        RuleSetResponse dto = new RuleSetResponse();
        dto.setId(ruleSet.getId());
        dto.setName(ruleSet.getName());
        dto.setDescription(ruleSet.getDescription());
        dto.setRuleIds(ruleSet.getRuleIds());
        dto.setCreatedAt(ruleSet.getCreatedAt());
        dto.setUpdatedAt(ruleSet.getUpdatedAt());
        return dto;
    }

    public static RuleSetResponse toResponse(RuleSet ruleSet, List<RuleResponse> rules) {
        RuleSetResponse dto = toResponse(ruleSet);
        dto.setRules(rules);
        return dto;
    }
}

