package com.datastd.rules.controller;

import com.datastd.common.dto.RuleResponse;
import com.datastd.common.dto.RuleSetResponse;
import com.datastd.rules.dto.RuleRequest;
import com.datastd.rules.dto.RuleSetRequest;
import com.datastd.rules.entity.StandardizationRule;
import com.datastd.rules.service.RuleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/rules")
public class RuleController {

    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    // === Standardization Rules ===

    @PostMapping
    public ResponseEntity<RuleResponse> createRule(@Valid @RequestBody RuleRequest request) {
        RuleResponse response = ruleService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<RuleResponse>> getAllRules(
            @RequestParam(required = false) String fieldName,
            @RequestParam(required = false) StandardizationRule.RuleType ruleType,
            @RequestParam(required = false) Boolean active) {
        List<RuleResponse> rules = ruleService.getAllRules(fieldName, ruleType, active);
        return ResponseEntity.ok(rules);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RuleResponse> getRuleById(@PathVariable UUID id) {
        RuleResponse response = ruleService.getRuleById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RuleResponse> updateRule(@PathVariable UUID id,
                                                    @Valid @RequestBody RuleRequest request) {
        RuleResponse response = ruleService.updateRule(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        ruleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<RuleResponse> toggleRule(@PathVariable UUID id) {
        RuleResponse response = ruleService.toggleRule(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/by-ids")
    public ResponseEntity<List<RuleResponse>> getRulesByIds(@RequestBody List<UUID> ids) {
        List<RuleResponse> rules = ruleService.getRulesByIds(ids);
        return ResponseEntity.ok(rules);
    }

    // === Rule Sets ===

    @PostMapping("/rulesets")
    public ResponseEntity<RuleSetResponse> createRuleSet(@Valid @RequestBody RuleSetRequest request) {
        RuleSetResponse response = ruleService.createRuleSet(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/rulesets")
    public ResponseEntity<List<RuleSetResponse>> getAllRuleSets() {
        List<RuleSetResponse> ruleSets = ruleService.getAllRuleSets();
        return ResponseEntity.ok(ruleSets);
    }

    @GetMapping("/rulesets/{id}")
    public ResponseEntity<RuleSetResponse> getRuleSetById(@PathVariable UUID id) {
        RuleSetResponse response = ruleService.getRuleSetById(id);
        return ResponseEntity.ok(response);
    }
}

