package com.datastd.standardization.service.rules;

import com.datastd.standardization.service.rules.impl.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

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
    void regex_invalidPattern_shouldReturnOriginal() {
        Map<String, Object> config = Map.of("pattern", "[invalid", "replacement", "");
        assertThat(new RegexApplier().apply("test", config)).isEqualTo("test");
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

    // ─── RuleApplierFactory ───────────────────────────────────────

    @Test
    void factory_knownType_shouldReturnApplier() {
        RuleApplierFactory factory = new RuleApplierFactory();
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
        RuleApplierFactory factory = new RuleApplierFactory();
        assertThatThrownBy(() -> factory.getApplier("UNKNOWN"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown rule type");
    }
}

