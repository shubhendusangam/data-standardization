package com.datastd.quality.engine;

import com.datastd.common.dto.ColumnReport;
import com.datastd.common.dto.InferredType;
import com.datastd.quality.dto.SuggestedRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for RuleSuggestionEngine — heuristic rule suggestion.
 */
class RuleSuggestionEngineTest {

    private RuleSuggestionEngine engine;

    @BeforeEach
    void setUp() {
        engine = new RuleSuggestionEngine(new ColumnProfiler());
    }

    @Test
    void suggest_emailColumn_suggestsRegexRule() {
        ColumnReport emailCol = buildColumn("email", InferredType.STRING, 0, 100, 10);
        List<SuggestedRule> suggestions = engine.suggest(List.of(emailCol));

        assertThat(suggestions).anyMatch(s ->
                s.getColumnName().equals("email")
                        && s.getValidationType().equals("REGEX_MATCH")
                        && s.getConfidence().equals("HIGH")
                        && s.getRationale() != null && !s.getRationale().isEmpty());
    }

    @Test
    void suggest_phoneColumn_suggestsRegexRule() {
        ColumnReport phoneCol = buildColumn("phone", InferredType.STRING, 0, 100, 10);
        List<SuggestedRule> suggestions = engine.suggest(List.of(phoneCol));

        assertThat(suggestions).anyMatch(s ->
                s.getColumnName().equals("phone")
                        && s.getValidationType().equals("REGEX_MATCH"));
    }

    @Test
    void suggest_nameColumn_suggestsNotEmpty() {
        ColumnReport nameCol = buildColumn("name", InferredType.STRING, 0, 100, 10);
        List<SuggestedRule> suggestions = engine.suggest(List.of(nameCol));

        assertThat(suggestions).anyMatch(s ->
                s.getColumnName().equals("name")
                        && s.getValidationType().equals("NOT_EMPTY")
                        && s.getSeverity().equals("ERROR"));
    }

    @Test
    void suggest_numericAgeColumn_suggestsRange() {
        ColumnReport ageCol = buildColumn("age", InferredType.INTEGER, 0, 100, 10);
        ageCol.setMin(18.0);
        ageCol.setMax(65.0);
        List<SuggestedRule> suggestions = engine.suggest(List.of(ageCol));

        assertThat(suggestions).anyMatch(s ->
                s.getColumnName().equals("age")
                        && s.getValidationType().equals("NUMERIC_RANGE")
                        && s.getParams().contains("18")
                        && s.getParams().contains("65"));
    }

    @Test
    void suggest_dateColumn_suggestsRegex() {
        ColumnReport dobCol = buildColumn("dob", InferredType.DATE, 0, 100, 10);
        dobCol.setSampleValues(List.of("2024-01-15", "2024-06-20", "2024-12-31"));
        List<SuggestedRule> suggestions = engine.suggest(List.of(dobCol));

        assertThat(suggestions).anyMatch(s ->
                s.getColumnName().equals("dob")
                        && s.getValidationType().equals("REGEX_MATCH")
                        && s.getRationale().contains("date"));
    }

    @Test
    void suggest_uniqueIdColumn_suggestsUnique() {
        ColumnReport idCol = buildColumn("id", InferredType.STRING, 0, 100, 10);
        // 100% unique
        List<SuggestedRule> suggestions = engine.suggest(List.of(idCol));

        assertThat(suggestions).anyMatch(s ->
                s.getColumnName().equals("id")
                        && s.getValidationType().equals("UNIQUE")
                        && s.getConfidence().equals("HIGH"));
    }

    @Test
    void suggest_columnWithNulls_suggestsNotNull() {
        ColumnReport col = buildColumn("address", InferredType.STRING, 15.0, 85, 10);
        List<SuggestedRule> suggestions = engine.suggest(List.of(col));

        assertThat(suggestions).anyMatch(s ->
                s.getColumnName().equals("address")
                        && s.getValidationType().equals("NOT_NULL")
                        && s.getConfidence().equals("LOW"));
    }

    @Test
    void suggest_genderColumn_suggestsAllowedValues() {
        ColumnReport genderCol = new ColumnReport();
        genderCol.setColumnName("gender");
        genderCol.setInferredType(InferredType.STRING);
        genderCol.setNullRatePct(0);
        genderCol.setUniqueRatePct(30);
        genderCol.setUniqueCount(3);
        genderCol.setTotalCount(10);
        genderCol.setSampleValues(List.of("Male", "Female", "Other"));
        genderCol.setTopValues(List.of(
                new ColumnReport.TopValue("Male", 5, 50),
                new ColumnReport.TopValue("Female", 3, 30),
                new ColumnReport.TopValue("Other", 2, 20)));

        List<SuggestedRule> suggestions = engine.suggest(List.of(genderCol));

        assertThat(suggestions).anyMatch(s ->
                s.getColumnName().equals("gender")
                        && s.getValidationType().equals("ALLOWED_VALUES")
                        && s.getParams().contains("Male"));
    }

    @Test
    void suggest_exampleCustomerDataset_returnsAtLeast3Suggestions() {
        // From acceptance criteria: name, gender, email, dob
        ColumnReport nameCol = buildColumn("name", InferredType.STRING, 0, 100, 4);
        ColumnReport genderCol = new ColumnReport();
        genderCol.setColumnName("gender");
        genderCol.setInferredType(InferredType.STRING);
        genderCol.setNullRatePct(0);
        genderCol.setUniqueRatePct(25);
        genderCol.setUniqueCount(1);
        genderCol.setTotalCount(4);
        genderCol.setSampleValues(List.of("M"));
        genderCol.setTopValues(List.of(new ColumnReport.TopValue("M", 4, 100)));

        ColumnReport emailCol = buildColumn("email", InferredType.STRING, 0, 100, 4);
        ColumnReport dobCol = buildColumn("dob", InferredType.DATE, 0, 100, 4);
        dobCol.setSampleValues(List.of("03/15/1990"));

        List<SuggestedRule> suggestions = engine.suggest(List.of(nameCol, genderCol, emailCol, dobCol));

        assertThat(suggestions.size()).isGreaterThanOrEqualTo(3);
        // Every suggestion must have a non-empty rationale
        for (SuggestedRule s : suggestions) {
            assertThat(s.getRationale()).isNotNull().isNotEmpty();
        }
    }

    @Test
    void suggest_everyRuleHasNonEmptyRationale() {
        ColumnReport col1 = buildColumn("email", InferredType.STRING, 5.0, 90, 10);
        ColumnReport col2 = buildColumn("age", InferredType.INTEGER, 0, 100, 10);
        col2.setMin(18.0);
        col2.setMax(65.0);

        List<SuggestedRule> suggestions = engine.suggest(List.of(col1, col2));
        for (SuggestedRule s : suggestions) {
            assertThat(s.getRationale()).as("Rationale for %s on %s", s.getValidationType(), s.getColumnName())
                    .isNotNull().isNotEmpty();
        }
    }

    // ── Helper ──────────────────────────────────────────────────────

    private ColumnReport buildColumn(String name, InferredType type, double nullRatePct,
                                     double uniqueRatePct, int totalCount) {
        ColumnReport col = new ColumnReport();
        col.setColumnName(name);
        col.setInferredType(type);
        col.setNullRatePct(nullRatePct);
        col.setUniqueRatePct(uniqueRatePct);
        col.setUniqueCount((int) (totalCount * uniqueRatePct / 100));
        col.setTotalCount(totalCount);
        col.setSampleValues(List.of());
        return col;
    }
}

