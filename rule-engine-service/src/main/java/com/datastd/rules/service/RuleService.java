package com.datastd.rules.service;

import com.datastd.common.dto.RuleResponse;
import com.datastd.common.dto.RuleSetResponse;
import com.datastd.rules.dto.RuleRequest;
import com.datastd.rules.dto.RuleSetRequest;
import com.datastd.rules.entity.StandardizationRule;

import java.util.List;
import java.util.UUID;

public interface RuleService {
    RuleResponse createRule(RuleRequest request);
    RuleResponse getRuleById(UUID id);
    List<RuleResponse> getAllRules(String fieldName, StandardizationRule.RuleType ruleType, Boolean active);
    RuleResponse updateRule(UUID id, RuleRequest request);
    void deleteRule(UUID id);
    RuleResponse toggleRule(UUID id);
    List<RuleResponse> getRulesByIds(List<UUID> ids);

    RuleSetResponse createRuleSet(RuleSetRequest request);
    List<RuleSetResponse> getAllRuleSets();
    RuleSetResponse getRuleSetById(UUID id);
}

