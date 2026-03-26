package com.datastd.standardization.service.rules;

import com.datastd.common.dto.RuleApplicationError;
import com.datastd.common.dto.RuleResponse;
import com.datastd.standardization.service.rules.impl.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.*;

class RuleExecutionEngineTest {

    private RuleExecutionEngine engine;

    @BeforeEach
    void setUp() {
        List<RuleApplier> appliers = List.of(
                new TrimApplier(), new UppercaseApplier(), new LowercaseApplier(),
                new ReplaceApplier(), new MapValuesApplier(), new RegexApplier(),
                new DefaultValueApplier(), new DateFormatApplier()
        );
        engine = new RuleExecutionEngine(new RuleApplierFactory(appliers), new ObjectMapper());
    }

    // ─── Helpers ──────────────────────────────────────────────────

    private static RuleResponse rule(String name, String fieldName, String ruleType,
                                     String ruleConfig, int priority) {
        RuleResponse r = new RuleResponse();
        r.setId(UUID.randomUUID());
        r.setName(name);
        r.setFieldName(fieldName);
        r.setRuleType(ruleType);
        r.setRuleConfig(ruleConfig);
        r.setPriority(priority);
        r.setActive(true);
        return r;
    }

    // ─── Basic rule application ───────────────────────────────────

    @Test
    void applyRules_validDateFormat_shouldTransformSuccessfully() {
        List<RuleResponse> rules = List.of(
                rule("Format DOB", "dob", "DATE_FORMAT",
                        "{\"sourceFormat\":\"MM/dd/yyyy\",\"targetFormat\":\"yyyy-MM-dd\"}", 1)
        );

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("name", "Alice");
        record.put("dob", "03/15/1990");

        RuleApplicationResult result = engine.applyRulesToRecord(record, rules);

        assertThat(result.getRecord().get("dob")).isEqualTo("1990-03-15");
        assertThat(result.getRecord().get("name")).isEqualTo("Alice");
        assertThat(result.hasErrors()).isFalse();
    }

    // ─── One failing rule keeps original value ────────────────────

    @Test
    void applyRules_unparseableDate_shouldKeepOriginalAndCollectError() {
        List<RuleResponse> rules = List.of(
                rule("Format DOB", "dob", "DATE_FORMAT",
                        "{\"sourceFormat\":\"MM/dd/yyyy\",\"targetFormat\":\"yyyy-MM-dd\"}", 1)
        );

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("name", "Bob");
        record.put("dob", "not-a-date");

        RuleApplicationResult result = engine.applyRulesToRecord(record, rules);

        // Original value retained
        assertThat(result.getRecord().get("dob")).isEqualTo("not-a-date");
        assertThat(result.getRecord().get("name")).isEqualTo("Bob");

        // Error collected
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(1);

        RuleApplicationError error = result.getErrors().get(0);
        assertThat(error.getFieldName()).isEqualTo("dob");
        assertThat(error.getOriginalValue()).isEqualTo("not-a-date");
        assertThat(error.getReason()).contains("Unparseable date");
        assertThat(error.getRuleName()).isEqualTo("Format DOB");
    }

    // ─── Failing rule on one field doesn't affect other fields ────

    @Test
    void applyRules_failingRuleOnOneField_otherFieldsProcessNormally() {
        List<RuleResponse> rules = List.of(
                rule("Uppercase Name", "name", "UPPERCASE", null, 1),
                rule("Format DOB", "dob", "DATE_FORMAT",
                        "{\"sourceFormat\":\"MM/dd/yyyy\",\"targetFormat\":\"yyyy-MM-dd\"}", 2)
        );

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("name", "charlie");
        record.put("dob", "garbage");

        RuleApplicationResult result = engine.applyRulesToRecord(record, rules);

        // Name was uppercased successfully
        assertThat(result.getRecord().get("name")).isEqualTo("CHARLIE");
        // DOB kept original
        assertThat(result.getRecord().get("dob")).isEqualTo("garbage");
        // Only one error
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getFieldName()).isEqualTo("dob");
    }

    // ─── Multiple records: error on record 5 doesn't affect others ─

    @Test
    void applyRules_multipleRecords_failureOnOneDoesNotAffectOthers() {
        List<RuleResponse> rules = List.of(
                rule("Format DOB", "dob", "DATE_FORMAT",
                        "{\"sourceFormat\":\"MM/dd/yyyy\",\"targetFormat\":\"yyyy-MM-dd\"}", 1)
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("id", i);
            rec.put("dob", i == 4 ? "bad-date" : "01/0" + (i + 1) + "/2020");
            records.add(rec);
        }

        List<RuleApplicationResult> results = new ArrayList<>();
        for (Map<String, Object> rec : records) {
            results.add(engine.applyRulesToRecord(rec, rules));
        }

        // Records 0-3 and 5-6 processed correctly
        for (int i = 0; i < 7; i++) {
            if (i == 4) {
                assertThat(results.get(i).getRecord().get("dob")).isEqualTo("bad-date");
                assertThat(results.get(i).hasErrors()).isTrue();
            } else {
                assertThat(results.get(i).getRecord().get("dob").toString()).startsWith("2020-01-");
                assertThat(results.get(i).hasErrors()).isFalse();
            }
        }
    }

    // ─── ISO date fallback ────────────────────────────────────────

    @Test
    void applyRules_isoDateAsInput_withMmDdSourceFormat_shouldReturnUnchanged() {
        List<RuleResponse> rules = List.of(
                rule("Format DOB", "dob", "DATE_FORMAT",
                        "{\"sourceFormat\":\"MM/dd/yyyy\",\"targetFormat\":\"yyyy-MM-dd\"}", 1)
        );

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("dob", "2023-12-25");

        RuleApplicationResult result = engine.applyRulesToRecord(record, rules);

        // Already in target format → returned unchanged, no error
        assertThat(result.getRecord().get("dob")).isEqualTo("2023-12-25");
        assertThat(result.hasErrors()).isFalse();
    }

    // ─── Inactive rules are skipped ───────────────────────────────

    @Test
    void applyRules_inactiveRule_shouldBeSkipped() {
        RuleResponse rule = rule("Uppercase", "name", "UPPERCASE", null, 1);
        rule.setActive(false);

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("name", "alice");

        RuleApplicationResult result = engine.applyRulesToRecord(record, List.of(rule));

        assertThat(result.getRecord().get("name")).isEqualTo("alice");
        assertThat(result.hasErrors()).isFalse();
    }
}

