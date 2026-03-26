package com.datastd.standardization.service.rules.impl;

import com.datastd.standardization.exception.RuleApplicationException;
import com.datastd.standardization.service.rules.RuleApplier;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Formats date values to a target format.
 * Config: {"sourceFormat": "MM/dd/yyyy", "targetFormat": "yyyy-MM-dd"}
 * If sourceFormat is not provided, attempts common formats.
 *
 * <p>When a {@code sourceFormat} is specified:
 * <ol>
 *   <li>Try parsing with {@code sourceFormat}.</li>
 *   <li>If that fails, try parsing with {@code targetFormat} (value may already be in target format).</li>
 *   <li>If both fail, throw {@link RuleApplicationException} so the caller can record the error
 *       and keep the original value.</li>
 * </ol>
 */
@Component
public class DateFormatApplier implements RuleApplier {

    private static final String[] COMMON_FORMATS = {
            "MM/dd/yyyy", "dd/MM/yyyy", "yyyy/MM/dd",
            "MM-dd-yyyy", "dd-MM-yyyy", "yyyy-MM-dd",
            "MMM dd, yyyy", "dd MMM yyyy",
            "MM/dd/yyyy HH:mm:ss", "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss"
    };

    @Override
    public String getType() {
        return "DATE_FORMAT";
    }

    @Override
    public String apply(String value, Map<String, Object> config) {
        if (value == null || value.trim().isEmpty() || config == null) return value;

        String targetFormat = (String) config.get("targetFormat");
        if (targetFormat == null) {
            targetFormat = "yyyy-MM-dd";
        }

        String sourceFormat = (String) config.get("sourceFormat");
        DateTimeFormatter targetFormatter = DateTimeFormatter.ofPattern(targetFormat);
        String raw = value.trim();

        if (sourceFormat != null && !sourceFormat.isEmpty()) {
            // 1. Try sourceFormat first
            try {
                DateTimeFormatter sourceFormatter = DateTimeFormatter.ofPattern(sourceFormat);
                LocalDate date = LocalDate.parse(raw, sourceFormatter);
                return date.format(targetFormatter);
            } catch (DateTimeParseException ignored) {}

            // 1b. sourceFormat with time component
            try {
                LocalDateTime dateTime = LocalDateTime.parse(raw,
                        DateTimeFormatter.ofPattern(sourceFormat));
                return dateTime.format(targetFormatter);
            } catch (DateTimeParseException ignored) {}

            // 2. If sourceFormat fails, try targetFormat (value may already be in target)
            try {
                LocalDate date = LocalDate.parse(raw, targetFormatter);
                return raw; // Already in target format — return unchanged
            } catch (DateTimeParseException ignored) {}

            try {
                LocalDateTime dateTime = LocalDateTime.parse(raw, targetFormatter);
                return raw; // Already in target format — return unchanged
            } catch (DateTimeParseException ignored) {}

            // 3. Both failed — throw RuleApplicationException (caught by RuleExecutionEngine)
            throw new RuleApplicationException(
                    "Unparseable date '" + raw + "' — tried formats: "
                            + sourceFormat + ", " + targetFormat);
        }

        // No sourceFormat specified — try common formats
        for (String format : COMMON_FORMATS) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                LocalDate date = LocalDate.parse(raw, formatter);
                return date.format(targetFormatter);
            } catch (DateTimeParseException ignored) {
            }
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                LocalDateTime dateTime = LocalDateTime.parse(raw, formatter);
                return dateTime.format(targetFormatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        // None of the common formats worked — throw
        throw new RuleApplicationException(
                "Unparseable date '" + raw + "' — none of the common formats matched");
    }
}

