package com.datastd.standardization.service.rules;

import com.datastd.standardization.exception.RuleApplicationException;
import com.datastd.standardization.service.rules.impl.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class RuleApplierTest {

    // ─── TrimApplier ──────────────────────────────────────────────

    @Test
    void trim_shouldRemoveWhitespace() {
        assertThat(new TrimApplier().apply("  hello  ", null)).isEqualTo("hello");
    }

    @Test
    void trim_null_shouldReturnNull() {
        assertThat(new TrimApplier().apply(null, null)).isNull();
    }

    // ─── UppercaseApplier ─────────────────────────────────────────

    @Test
    void uppercase_shouldConvert() {
        assertThat(new UppercaseApplier().apply("hello", null)).isEqualTo("HELLO");
    }

    @Test
    void uppercase_null_shouldReturnNull() {
        assertThat(new UppercaseApplier().apply(null, null)).isNull();
    }

    // ─── LowercaseApplier ─────────────────────────────────────────

    @Test
    void lowercase_shouldConvert() {
        assertThat(new LowercaseApplier().apply("HELLO", null)).isEqualTo("hello");
    }

    // ─── ReplaceApplier ───────────────────────────────────────────

    @Test
    void replace_shouldFindAndReplace() {
        Map<String, Object> config = Map.of("find", "old", "replace", "new");
        assertThat(new ReplaceApplier().apply("old text", config)).isEqualTo("new text");
    }

    @Test
    void replace_nullConfig_shouldReturnOriginal() {
        assertThat(new ReplaceApplier().apply("abc", null)).isEqualTo("abc");
    }

    @Test
    void replace_nullValue_shouldReturnNull() {
        assertThat(new ReplaceApplier().apply(null, Map.of("find", "x"))).isNull();
    }

    // ─── MapValuesApplier ─────────────────────────────────────────

    @Test
    void mapValues_shouldMapKnownValue() {
        Map<String, Object> config = Map.of("mappings", Map.of("M", "Male", "F", "Female"));
        assertThat(new MapValuesApplier().apply("M", config)).isEqualTo("Male");
    }

    @Test
    void mapValues_unknownValue_shouldReturnOriginal() {
        Map<String, Object> config = Map.of("mappings", Map.of("M", "Male"));
        assertThat(new MapValuesApplier().apply("X", config)).isEqualTo("X");
    }

    // ─── RegexApplier ─────────────────────────────────────────────

    @Test
    void regex_shouldReplacePattern() {
        Map<String, Object> config = Map.of("pattern", "[^0-9]", "replacement", "");
        assertThat(new RegexApplier().apply("abc123def", config)).isEqualTo("123");
    }

    @Test
    void regex_invalidPattern_shouldThrowRuleApplicationException() {
        Map<String, Object> config = Map.of("pattern", "[invalid", "replacement", "");
        assertThatThrownBy(() -> new RegexApplier().apply("test", config))
                .isInstanceOf(RuleApplicationException.class)
                .hasMessageContaining("Invalid regex pattern");
    }

    // ─── DefaultValueApplier ──────────────────────────────────────

    @Test
    void defaultValue_null_shouldSetDefault() {
        Map<String, Object> config = Map.of("defaultValue", "N/A");
        assertThat(new DefaultValueApplier().apply(null, config)).isEqualTo("N/A");
    }

    @Test
    void defaultValue_empty_shouldSetDefault() {
        Map<String, Object> config = Map.of("defaultValue", "N/A");
        assertThat(new DefaultValueApplier().apply("  ", config)).isEqualTo("N/A");
    }

    @Test
    void defaultValue_hasValue_shouldKeepOriginal() {
        Map<String, Object> config = Map.of("defaultValue", "N/A");
        assertThat(new DefaultValueApplier().apply("hello", config)).isEqualTo("hello");
    }

    // ─── DateFormatApplier ────────────────────────────────────────

    @Test
    void dateFormat_shouldConvert() {
        Map<String, Object> config = Map.of(
                "sourceFormat", "MM/dd/yyyy",
                "targetFormat", "yyyy-MM-dd"
        );
        assertThat(new DateFormatApplier().apply("03/15/1990", config)).isEqualTo("1990-03-15");
    }

    @Test
    void dateFormat_nullValue_shouldReturnNull() {
        Map<String, Object> config = Map.of("targetFormat", "yyyy-MM-dd");
        assertThat(new DateFormatApplier().apply(null, config)).isNull();
    }

    @Test
    void dateFormat_isoInputWithMmDdSourceFormat_shouldReturnOriginalUnchanged() {
        // Value already in target format (yyyy-MM-dd) with sourceFormat=MM/dd/yyyy
        // → fallback should recognise it and return unchanged
        Map<String, Object> config = Map.of(
                "sourceFormat", "MM/dd/yyyy",
                "targetFormat", "yyyy-MM-dd"
        );
        assertThat(new DateFormatApplier().apply("1990-03-15", config)).isEqualTo("1990-03-15");
    }

    @Test
    void dateFormat_validMmDdInput_shouldReturnCorrectIso() {
        Map<String, Object> config = Map.of(
                "sourceFormat", "MM/dd/yyyy",
                "targetFormat", "yyyy-MM-dd"
        );
        assertThat(new DateFormatApplier().apply("12/25/2023", config)).isEqualTo("2023-12-25");
    }

    @Test
    void dateFormat_completelyUnparseable_shouldThrowRuleApplicationException() {
        Map<String, Object> config = Map.of(
                "sourceFormat", "MM/dd/yyyy",
                "targetFormat", "yyyy-MM-dd"
        );
        assertThatThrownBy(() -> new DateFormatApplier().apply("not-a-date", config))
                .isInstanceOf(RuleApplicationException.class)
                .hasMessageContaining("Unparseable date 'not-a-date'")
                .hasMessageContaining("MM/dd/yyyy")
                .hasMessageContaining("yyyy-MM-dd");
    }

    @Test
    void dateFormat_noSourceFormat_unparseableValue_shouldThrowRuleApplicationException() {
        Map<String, Object> config = Map.of("targetFormat", "yyyy-MM-dd");
        assertThatThrownBy(() -> new DateFormatApplier().apply("not-a-date", config))
                .isInstanceOf(RuleApplicationException.class)
                .hasMessageContaining("none of the common formats matched");
    }

    @Test
    void dateFormat_emptyValue_shouldReturnEmpty() {
        Map<String, Object> config = Map.of(
                "sourceFormat", "MM/dd/yyyy",
                "targetFormat", "yyyy-MM-dd"
        );
        assertThat(new DateFormatApplier().apply("  ", config)).isEqualTo("  ");
    }

    // ─── RuleApplierFactory ───────────────────────────────────────

    private RuleApplierFactory createFactory() {
        List<RuleApplier> appliers = List.of(
                new TrimApplier(), new UppercaseApplier(), new LowercaseApplier(),
                new ReplaceApplier(), new MapValuesApplier(), new RegexApplier(),
                new DefaultValueApplier(), new DateFormatApplier()
        );
        return new RuleApplierFactory(appliers);
    }

    @Test
    void factory_knownType_shouldReturnApplier() {
        RuleApplierFactory factory = createFactory();
        assertThat(factory.getApplier("TRIM")).isInstanceOf(TrimApplier.class);
        assertThat(factory.getApplier("UPPERCASE")).isInstanceOf(UppercaseApplier.class);
        assertThat(factory.getApplier("LOWERCASE")).isInstanceOf(LowercaseApplier.class);
        assertThat(factory.getApplier("REPLACE")).isInstanceOf(ReplaceApplier.class);
        assertThat(factory.getApplier("MAP_VALUES")).isInstanceOf(MapValuesApplier.class);
        assertThat(factory.getApplier("REGEX")).isInstanceOf(RegexApplier.class);
        assertThat(factory.getApplier("DEFAULT_VALUE")).isInstanceOf(DefaultValueApplier.class);
        assertThat(factory.getApplier("DATE_FORMAT")).isInstanceOf(DateFormatApplier.class);
    }

    @Test
    void factory_unknownType_shouldThrow() {
        RuleApplierFactory factory = createFactory();
        assertThatThrownBy(() -> factory.getApplier("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown rule type");
    }
}

