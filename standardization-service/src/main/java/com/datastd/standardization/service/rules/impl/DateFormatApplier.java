package com.datastd.standardization.service.rules.impl;

import com.datastd.standardization.service.rules.RuleApplier;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Formats date values to a target format.
 * Config: {"sourceFormat": "MM/dd/yyyy", "targetFormat": "yyyy-MM-dd"}
 * If sourceFormat is not provided, attempts common formats.
 */
public class DateFormatApplier implements RuleApplier {

    private static final String[] COMMON_FORMATS = {
            "MM/dd/yyyy", "dd/MM/yyyy", "yyyy/MM/dd",
            "MM-dd-yyyy", "dd-MM-yyyy", "yyyy-MM-dd",
            "MMM dd, yyyy", "dd MMM yyyy",
            "MM/dd/yyyy HH:mm:ss", "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss"
    };

    @Override
    public String apply(String value, Map<String, Object> config) {
        if (value == null || value.trim().isEmpty() || config == null) return value;

        String targetFormat = (String) config.get("targetFormat");
        if (targetFormat == null) {
            targetFormat = "yyyy-MM-dd";
        }

        String sourceFormat = (String) config.get("sourceFormat");
        DateTimeFormatter targetFormatter = DateTimeFormatter.ofPattern(targetFormat);

        if (sourceFormat != null && !sourceFormat.isEmpty()) {
            try {
                DateTimeFormatter sourceFormatter = DateTimeFormatter.ofPattern(sourceFormat);
                LocalDate date = LocalDate.parse(value.trim(), sourceFormatter);
                return date.format(targetFormatter);
            } catch (DateTimeParseException e) {
                try {
                    LocalDateTime dateTime = LocalDateTime.parse(value.trim(),
                            DateTimeFormatter.ofPattern(sourceFormat));
                    return dateTime.format(targetFormatter);
                } catch (DateTimeParseException e2) {
                    return value;
                }
            }
        }

        // Try common formats
        for (String format : COMMON_FORMATS) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                LocalDate date = LocalDate.parse(value.trim(), formatter);
                return date.format(targetFormatter);
            } catch (DateTimeParseException ignored) {
            }
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                LocalDateTime dateTime = LocalDateTime.parse(value.trim(), formatter);
                return dateTime.format(targetFormatter);
            } catch (DateTimeParseException ignored) {
            }
        }

        return value;
    }
}

