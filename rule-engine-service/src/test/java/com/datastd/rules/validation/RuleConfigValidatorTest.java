package com.datastd.rules.validation;

import com.datastd.rules.entity.StandardizationRule.RuleType;
import com.datastd.rules.exception.RuleConfigException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class RuleConfigValidatorTest {

    private RuleConfigValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RuleConfigValidator(new ObjectMapper());
    }

    // ─── Valid configs ────────────────────────────────────────────

    @Test
    void trim_withEmptyObject_shouldNotThrow() {
        assertThatNoException().isThrownBy(() ->
                validator.validate(RuleType.TRIM, "{}"));
    }

    @Test
    void trim_withNull_shouldNotThrow() {
        assertThatNoException().isThrownBy(() ->
                validator.validate(RuleType.TRIM, null));
    }

    @Test
    void trim_withBlank_shouldNotThrow() {
        assertThatNoException().isThrownBy(() ->
                validator.validate(RuleType.TRIM, "  "));
    }

    @Test
    void uppercase_withEmptyObject_shouldNotThrow() {
        assertThatNoException().isThrownBy(() ->
                validator.validate(RuleType.UPPERCASE, "{}"));
    }

    @Test
    void lowercase_withEmptyObject_shouldNotThrow() {
        assertThatNoException().isThrownBy(() ->
                validator.validate(RuleType.LOWERCASE, "{}"));
    }

    @Test
    void replace_validConfig_shouldNotThrow() {
        String config = """
                {"find": "old", "replace": "new"}
                """;
        assertThatNoException().isThrownBy(() ->
                validator.validate(RuleType.REPLACE, config));
    }

    @Test
    void mapValues_validConfig_shouldNotThrow() {
        String config = """
                {"mappings": {"M": "Male", "F": "Female"}}
                """;
        assertThatNoException().isThrownBy(() ->
                validator.validate(RuleType.MAP_VALUES, config));
    }

    @Test
    void regex_validConfig_shouldNotThrow() {
        String config = """
                {"pattern": "\\\\d+", "replacement": "#"}
                """;
        assertThatNoException().isThrownBy(() ->
                validator.validate(RuleType.REGEX, config));
    }

    @Test
    void defaultValue_validConfig_shouldNotThrow() {
        String config = """
                {"defaultValue": "N/A"}
                """;
        assertThatNoException().isThrownBy(() ->
                validator.validate(RuleType.DEFAULT_VALUE, config));
    }

    @Test
    void dateFormat_validConfig_shouldNotThrow() {
        String config = """
                {"sourceFormat": "MM/dd/yyyy", "targetFormat": "yyyy-MM-dd"}
                """;
        assertThatNoException().isThrownBy(() ->
                validator.validate(RuleType.DATE_FORMAT, config));
    }

    // ─── Missing required keys ────────────────────────────────────

    @Test
    void replace_missingFind_shouldThrowWithKeyName() {
        String config = """
                {"fin": "old", "replace": "new"}
                """;
        assertThatThrownBy(() -> validator.validate(RuleType.REPLACE, config))
                .isInstanceOf(RuleConfigException.class)
                .hasMessageContaining("find");
    }

    @Test
    void replace_missingReplace_shouldThrowWithKeyName() {
        String config = """
                {"find": "old"}
                """;
        assertThatThrownBy(() -> validator.validate(RuleType.REPLACE, config))
                .isInstanceOf(RuleConfigException.class)
                .hasMessageContaining("replace");
    }

    @Test
    void mapValues_missingMappings_shouldThrowWithKeyName() {
        String config = """
                {"mapping": {"M": "Male"}}
                """;
        assertThatThrownBy(() -> validator.validate(RuleType.MAP_VALUES, config))
                .isInstanceOf(RuleConfigException.class)
                .hasMessageContaining("mappings");
    }

    @Test
    void mapValues_mappingsNotObject_shouldThrow() {
        String config = """
                {"mappings": ["M", "Male"]}
                """;
        assertThatThrownBy(() -> validator.validate(RuleType.MAP_VALUES, config))
                .isInstanceOf(RuleConfigException.class)
                .hasMessageContaining("must be a JSON object");
    }

    @Test
    void mapValues_mappingsIsString_shouldThrow() {
        String config = """
                {"mappings": "not-an-object"}
                """;
        assertThatThrownBy(() -> validator.validate(RuleType.MAP_VALUES, config))
                .isInstanceOf(RuleConfigException.class)
                .hasMessageContaining("must be a JSON object");
    }

    @Test
    void regex_missingPattern_shouldThrowWithKeyName() {
        String config = """
                {"replacement": "#"}
                """;
        assertThatThrownBy(() -> validator.validate(RuleType.REGEX, config))
                .isInstanceOf(RuleConfigException.class)
                .hasMessageContaining("pattern");
    }

    @Test
    void regex_missingReplacement_shouldThrowWithKeyName() {
        String config = """
                {"pattern": "\\\\d+"}
                """;
        assertThatThrownBy(() -> validator.validate(RuleType.REGEX, config))
                .isInstanceOf(RuleConfigException.class)
                .hasMessageContaining("replacement");
    }

    @Test
    void defaultValue_missingDefaultValue_shouldThrowWithKeyName() {
        String config = """
                {"value": "N/A"}
                """;
        assertThatThrownBy(() -> validator.validate(RuleType.DEFAULT_VALUE, config))
                .isInstanceOf(RuleConfigException.class)
                .hasMessageContaining("defaultValue");
    }

    @Test
    void dateFormat_missingSourceFormat_shouldThrowWithKeyName() {
        String config = """
                {"targetFormat": "yyyy-MM-dd"}
                """;
        assertThatThrownBy(() -> validator.validate(RuleType.DATE_FORMAT, config))
                .isInstanceOf(RuleConfigException.class)
                .hasMessageContaining("sourceFormat");
    }

    @Test
    void dateFormat_missingTargetFormat_shouldThrowWithKeyName() {
        String config = """
                {"sourceFormat": "MM/dd/yyyy"}
                """;
        assertThatThrownBy(() -> validator.validate(RuleType.DATE_FORMAT, config))
                .isInstanceOf(RuleConfigException.class)
                .hasMessageContaining("targetFormat");
    }

    // ─── Malformed / missing JSON ─────────────────────────────────

    @Test
    void malformedJson_shouldThrowWithInvalidJsonMessage() {
        assertThatThrownBy(() -> validator.validate(RuleType.REPLACE, "not-json"))
                .isInstanceOf(RuleConfigException.class)
                .hasMessageContaining("invalid JSON");
    }

    @Test
    void nullConfig_forTypeRequiringConfig_shouldThrow() {
        assertThatThrownBy(() -> validator.validate(RuleType.REPLACE, null))
                .isInstanceOf(RuleConfigException.class)
                .hasMessageContaining("ruleConfig is required");
    }

    @Test
    void blankConfig_forTypeRequiringConfig_shouldThrow() {
        assertThatThrownBy(() -> validator.validate(RuleType.DEFAULT_VALUE, "  "))
                .isInstanceOf(RuleConfigException.class)
                .hasMessageContaining("ruleConfig is required");
    }

    @Test
    void arrayInsteadOfObject_shouldThrow() {
        assertThatThrownBy(() -> validator.validate(RuleType.REPLACE, "[1,2,3]"))
                .isInstanceOf(RuleConfigException.class)
                .hasMessageContaining("must be a JSON object");
    }

    // ─── ruleType carried in exception ────────────────────────────

    @Test
    void exception_shouldCarryRuleType() {
        try {
            validator.validate(RuleType.REPLACE, """
                    {"fin": "old", "replace": "new"}
                    """);
            fail("Expected RuleConfigException");
        } catch (RuleConfigException ex) {
            assertThat(ex.getRuleType()).isEqualTo("REPLACE");
            assertThat(ex.getMessage()).contains("find");
        }
    }

    @Test
    void nullKeyValue_shouldBeTreatedAsMissing() {
        String config = """
                {"find": null, "replace": "new"}
                """;
        assertThatThrownBy(() -> validator.validate(RuleType.REPLACE, config))
                .isInstanceOf(RuleConfigException.class)
                .hasMessageContaining("find");
    }
}


