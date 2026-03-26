package com.datastd.quality.engine.validation;

import com.datastd.quality.entity.ValidationType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory that maps {@link ValidationType} to the corresponding {@link ValidationStrategy}.
 * Uses Spring-injected strategy beans for auto-discovery — adding a new validation type
 * only requires creating a new {@code @Component} implementing {@link ValidationStrategy}
 * and registering it here.
 */
@Component
public class ValidationStrategyFactory {

    private final Map<ValidationType, ValidationStrategy> strategies;

    public ValidationStrategyFactory(
            NotNullValidation notNullValidation,
            NotEmptyValidation notEmptyValidation,
            RegexMatchValidation regexMatchValidation,
            AllowedValuesValidation allowedValuesValidation,
            NumericRangeValidation numericRangeValidation,
            MinLengthValidation minLengthValidation,
            MaxLengthValidation maxLengthValidation,
            UniqueValidation uniqueValidation,
            CustomSqlValidation customSqlValidation) {

        strategies = new EnumMap<>(ValidationType.class);
        strategies.put(ValidationType.NOT_NULL, notNullValidation);
        strategies.put(ValidationType.NOT_EMPTY, notEmptyValidation);
        strategies.put(ValidationType.REGEX_MATCH, regexMatchValidation);
        strategies.put(ValidationType.ALLOWED_VALUES, allowedValuesValidation);
        strategies.put(ValidationType.NUMERIC_RANGE, numericRangeValidation);
        strategies.put(ValidationType.MIN_LENGTH, minLengthValidation);
        strategies.put(ValidationType.MAX_LENGTH, maxLengthValidation);
        strategies.put(ValidationType.UNIQUE, uniqueValidation);
        strategies.put(ValidationType.CUSTOM_SQL, customSqlValidation);
    }

    /**
     * Returns the strategy for the given validation type.
     *
     * @throws IllegalArgumentException if no strategy is registered for the type
     */
    public ValidationStrategy getStrategy(ValidationType type) {
        ValidationStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalArgumentException("No validation strategy registered for type: " + type);
        }
        return strategy;
    }
}

