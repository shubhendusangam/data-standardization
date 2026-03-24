package com.datastd.rules.repository;

import com.datastd.rules.entity.StandardizationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StandardizationRuleRepository extends JpaRepository<StandardizationRule, UUID> {
    List<StandardizationRule> findByFieldName(String fieldName);
    List<StandardizationRule> findByRuleType(StandardizationRule.RuleType ruleType);
    List<StandardizationRule> findAllByActiveTrue();
    List<StandardizationRule> findByActiveOrderByPriorityAsc(boolean active);
    List<StandardizationRule> findAllByOrderByPriorityAsc();
    List<StandardizationRule> findByIdInOrderByPriorityAsc(List<UUID> ids);
}

