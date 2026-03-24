package com.datastd.rules.service.impl;

import com.datastd.common.dto.RuleResponse;
import com.datastd.common.dto.RuleSetResponse;
import com.datastd.rules.dto.RuleRequest;
import com.datastd.rules.dto.RuleSetRequest;
import com.datastd.rules.entity.RuleSet;
import com.datastd.rules.entity.StandardizationRule;
import com.datastd.rules.mapper.RuleMapper;
import com.datastd.rules.repository.RuleSetRepository;
import com.datastd.rules.repository.StandardizationRuleRepository;
import com.datastd.rules.service.RuleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RuleServiceImpl implements RuleService {

    private static final Logger log = LoggerFactory.getLogger(RuleServiceImpl.class);

    private final StandardizationRuleRepository ruleRepository;
    private final RuleSetRepository ruleSetRepository;

    public RuleServiceImpl(StandardizationRuleRepository ruleRepository,
                           RuleSetRepository ruleSetRepository) {
        this.ruleRepository = ruleRepository;
        this.ruleSetRepository = ruleSetRepository;
    }

    @Override
    public RuleResponse createRule(RuleRequest request) {
        log.info("Creating rule: name={}, field={}, type={}, priority={}",
                request.getName(), request.getFieldName(), request.getRuleType(), request.getPriority());
        StandardizationRule rule = new StandardizationRule();
        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setFieldName(request.getFieldName());
        rule.setRuleType(request.getRuleType());
        rule.setRuleConfig(request.getRuleConfig());
        rule.setPriority(request.getPriority());
        rule.setActive(request.isActive());

        StandardizationRule saved = ruleRepository.save(rule);
        log.info("Rule created: id={}, name={}", saved.getId(), saved.getName());
        return RuleMapper.toResponse(saved);
    }

    @Override
    public RuleResponse getRuleById(UUID id) {
        log.debug("Fetching rule: id={}", id);
        StandardizationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Rule not found: id={}", id);
                    return new RuntimeException("Rule not found with id: " + id);
                });
        return RuleMapper.toResponse(rule);
    }

    @Override
    public List<RuleResponse> getAllRules(String fieldName, StandardizationRule.RuleType ruleType, Boolean active) {
        log.debug("Listing rules: fieldName={}, ruleType={}, active={}", fieldName, ruleType, active);
        List<StandardizationRule> rules;

        if (fieldName != null) {
            rules = ruleRepository.findByFieldName(fieldName);
        } else if (ruleType != null) {
            rules = ruleRepository.findByRuleType(ruleType);
        } else if (active != null) {
            rules = ruleRepository.findByActiveOrderByPriorityAsc(active);
        } else {
            rules = ruleRepository.findAllByOrderByPriorityAsc();
        }

        return rules.stream()
                .map(RuleMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RuleResponse updateRule(UUID id, RuleRequest request) {
        log.info("Updating rule: id={}", id);
        StandardizationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Update failed — rule not found: id={}", id);
                    return new RuntimeException("Rule not found with id: " + id);
                });

        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setFieldName(request.getFieldName());
        rule.setRuleType(request.getRuleType());
        rule.setRuleConfig(request.getRuleConfig());
        rule.setPriority(request.getPriority());
        rule.setActive(request.isActive());

        StandardizationRule saved = ruleRepository.save(rule);
        log.info("Rule updated: id={}, name={}", saved.getId(), saved.getName());
        return RuleMapper.toResponse(saved);
    }

    @Override
    public void deleteRule(UUID id) {
        if (!ruleRepository.existsById(id)) {
            log.warn("Delete failed — rule not found: id={}", id);
            throw new RuntimeException("Rule not found with id: " + id);
        }
        ruleRepository.deleteById(id);
        log.info("Rule deleted: id={}", id);
    }

    @Override
    public RuleResponse toggleRule(UUID id) {
        StandardizationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rule not found with id: " + id));
        rule.setActive(!rule.isActive());
        StandardizationRule saved = ruleRepository.save(rule);
        log.info("Rule toggled: id={}, active={}", saved.getId(), saved.isActive());
        return RuleMapper.toResponse(saved);
    }

    @Override
    public List<RuleResponse> getRulesByIds(List<UUID> ids) {
        log.debug("Fetching rules by IDs: count={}", ids.size());
        List<StandardizationRule> rules = ruleRepository.findByIdInOrderByPriorityAsc(ids);
        return rules.stream()
                .map(RuleMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RuleSetResponse createRuleSet(RuleSetRequest request) {
        log.info("Creating rule set: name={}, ruleCount={}", request.getName(), request.getRuleIds().size());
        // Validate all rule IDs exist
        List<StandardizationRule> rules = ruleRepository.findByIdInOrderByPriorityAsc(request.getRuleIds());
        if (rules.size() != request.getRuleIds().size()) {
            throw new RuntimeException("Some rule IDs are invalid");
        }

        RuleSet ruleSet = new RuleSet();
        ruleSet.setName(request.getName());
        ruleSet.setDescription(request.getDescription());
        ruleSet.setRuleIds(request.getRuleIds());

        RuleSet saved = ruleSetRepository.save(ruleSet);
        List<RuleResponse> ruleResponses = rules.stream()
                .map(RuleMapper::toResponse)
                .collect(Collectors.toList());
        log.info("Rule set created: id={}, name={}", saved.getId(), saved.getName());
        return RuleMapper.toResponse(saved, ruleResponses);
    }

    @Override
    public List<RuleSetResponse> getAllRuleSets() {
        log.debug("Listing all rule sets");
        return ruleSetRepository.findAll().stream()
                .map(RuleMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public RuleSetResponse getRuleSetById(UUID id) {
        log.debug("Fetching rule set: id={}", id);
        RuleSet ruleSet = ruleSetRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Rule set not found: id={}", id);
                    return new RuntimeException("RuleSet not found with id: " + id);
                });

        List<StandardizationRule> rules = ruleRepository.findByIdInOrderByPriorityAsc(ruleSet.getRuleIds());
        List<RuleResponse> ruleResponses = rules.stream()
                .map(RuleMapper::toResponse)
                .collect(Collectors.toList());

        return RuleMapper.toResponse(ruleSet, ruleResponses);
    }
}

