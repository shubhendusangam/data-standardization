package com.datastd.rules.service.impl;

import com.datastd.common.dto.RuleResponse;
import com.datastd.common.dto.RuleSetResponse;
import com.datastd.rules.dto.RuleRequest;
import com.datastd.rules.dto.RuleSetRequest;
import com.datastd.rules.entity.RuleSet;
import com.datastd.rules.entity.StandardizationRule;
import com.datastd.rules.entity.StandardizationRule.RuleType;
import com.datastd.rules.repository.RuleSetRepository;
import com.datastd.rules.repository.StandardizationRuleRepository;
import com.datastd.rules.validation.RuleConfigValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RuleServiceImplTest {

    @Mock
    private StandardizationRuleRepository ruleRepository;

    @Mock
    private RuleSetRepository ruleSetRepository;

    @Mock
    private RuleConfigValidator ruleConfigValidator;

    @InjectMocks
    private RuleServiceImpl ruleService;

    // ─── createRule ───────────────────────────────────────────────

    @Test
    void createRule_shouldSaveAndReturnResponse() {
        RuleRequest request = buildRuleRequest("Trim Name", "name", RuleType.TRIM, 1);

        StandardizationRule saved = buildRuleEntity(request);
        when(ruleRepository.save(any())).thenReturn(saved);

        RuleResponse result = ruleService.createRule(request);

        assertThat(result.getName()).isEqualTo("Trim Name");
        assertThat(result.getRuleType()).isEqualTo("TRIM");
        assertThat(result.getFieldName()).isEqualTo("name");
        verify(ruleRepository).save(any());
    }

    // ─── getRuleById ──────────────────────────────────────────────

    @Test
    void getRuleById_found_shouldReturn() {
        UUID id = UUID.randomUUID();
        StandardizationRule rule = buildRuleEntity(buildRuleRequest("R1", "f", RuleType.UPPERCASE, 0));
        rule.setId(id);
        when(ruleRepository.findById(id)).thenReturn(Optional.of(rule));

        RuleResponse result = ruleService.getRuleById(id);

        assertThat(result.getId()).isEqualTo(id);
    }

    @Test
    void getRuleById_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(ruleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ruleService.getRuleById(id))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Rule not found");
    }

    // ─── getAllRules with filters ─────────────────────────────────

    @Test
    void getAllRules_noFilter_shouldReturnAll() {
        when(ruleRepository.findAllByOrderByPriorityAsc()).thenReturn(List.of());

        List<RuleResponse> result = ruleService.getAllRules(null, null, null);

        assertThat(result).isEmpty();
        verify(ruleRepository).findAllByOrderByPriorityAsc();
    }

    @Test
    void getAllRules_filterByFieldName() {
        when(ruleRepository.findByFieldName("email")).thenReturn(List.of());

        ruleService.getAllRules("email", null, null);

        verify(ruleRepository).findByFieldName("email");
    }

    @Test
    void getAllRules_filterByActive() {
        when(ruleRepository.findByActiveOrderByPriorityAsc(true)).thenReturn(List.of());

        ruleService.getAllRules(null, null, true);

        verify(ruleRepository).findByActiveOrderByPriorityAsc(true);
    }

    @Test
    void getAllRules_filterByActiveFalse() {
        when(ruleRepository.findByActiveOrderByPriorityAsc(false)).thenReturn(List.of());

        ruleService.getAllRules(null, null, false);

        verify(ruleRepository).findByActiveOrderByPriorityAsc(false);
    }

    // ─── toggleRule ───────────────────────────────────────────────

    @Test
    void toggleRule_shouldFlipActiveFlag() {
        UUID id = UUID.randomUUID();
        StandardizationRule rule = buildRuleEntity(buildRuleRequest("R", "f", RuleType.TRIM, 0));
        rule.setId(id);
        rule.setActive(true);
        when(ruleRepository.findById(id)).thenReturn(Optional.of(rule));
        when(ruleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RuleResponse result = ruleService.toggleRule(id);

        assertThat(result.isActive()).isFalse();
    }

    // ─── deleteRule ───────────────────────────────────────────────

    @Test
    void deleteRule_exists_shouldDelete() {
        UUID id = UUID.randomUUID();
        when(ruleRepository.existsById(id)).thenReturn(true);

        ruleService.deleteRule(id);

        verify(ruleRepository).deleteById(id);
    }

    @Test
    void deleteRule_notFound_shouldThrow() {
        UUID id = UUID.randomUUID();
        when(ruleRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> ruleService.deleteRule(id))
                .isInstanceOf(RuntimeException.class);
    }

    // ─── createRuleSet ────────────────────────────────────────────

    @Test
    void createRuleSet_validIds_shouldCreate() {
        UUID rId = UUID.randomUUID();
        StandardizationRule rule = buildRuleEntity(buildRuleRequest("R1", "f", RuleType.TRIM, 0));
        rule.setId(rId);

        when(ruleRepository.findByIdInOrderByPriorityAsc(List.of(rId))).thenReturn(List.of(rule));
        when(ruleSetRepository.save(any())).thenAnswer(inv -> {
            RuleSet rs = inv.getArgument(0);
            rs.setId(UUID.randomUUID());
            rs.setCreatedAt(LocalDateTime.now());
            rs.setUpdatedAt(LocalDateTime.now());
            return rs;
        });

        RuleSetRequest req = new RuleSetRequest();
        req.setName("My Set");
        req.setRuleIds(List.of(rId));

        RuleSetResponse result = ruleService.createRuleSet(req);

        assertThat(result.getName()).isEqualTo("My Set");
        assertThat(result.getRules()).hasSize(1);
    }

    @Test
    void createRuleSet_invalidIds_shouldThrow() {
        UUID rId = UUID.randomUUID();
        when(ruleRepository.findByIdInOrderByPriorityAsc(List.of(rId))).thenReturn(List.of());

        RuleSetRequest req = new RuleSetRequest();
        req.setName("Bad Set");
        req.setRuleIds(List.of(rId));

        assertThatThrownBy(() -> ruleService.createRuleSet(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("invalid");
    }

    // ─── Helpers ──────────────────────────────────────────────────

    private RuleRequest buildRuleRequest(String name, String field, RuleType type, int priority) {
        RuleRequest r = new RuleRequest();
        r.setName(name);
        r.setFieldName(field);
        r.setRuleType(type);
        r.setPriority(priority);
        r.setActive(true);
        return r;
    }

    private StandardizationRule buildRuleEntity(RuleRequest request) {
        StandardizationRule e = new StandardizationRule();
        e.setId(UUID.randomUUID());
        e.setName(request.getName());
        e.setFieldName(request.getFieldName());
        e.setRuleType(request.getRuleType());
        e.setPriority(request.getPriority());
        e.setActive(request.isActive());
        e.setRuleConfig(request.getRuleConfig());
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        return e;
    }
}

