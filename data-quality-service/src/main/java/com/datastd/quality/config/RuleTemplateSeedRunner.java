package com.datastd.quality.config;

import com.datastd.quality.entity.Severity;
import com.datastd.quality.entity.ValidationRule;
import com.datastd.quality.entity.ValidationType;
import com.datastd.quality.repository.ValidationRuleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds template validation rules on application startup if the table is empty.
 * These are pre-built rule suggestions applicable to common data patterns.
 * The rules are created as active=false (templates only) so they don't
 * interfere with existing validation unless explicitly enabled.
 */
@Component
public class RuleTemplateSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RuleTemplateSeedRunner.class);

    private final ValidationRuleRepository ruleRepository;

    public RuleTemplateSeedRunner(ValidationRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (ruleRepository.count() > 0) {
            log.info("Validation rules already exist, skipping template seeding");
            return;
        }

        log.info("Seeding template validation rules...");

        seed("Email format", "email", ValidationType.REGEX_MATCH,
                "{\"pattern\":\"^[\\\\w.+]+@[\\\\w.]+\\\\.[a-z]{2,}$\",\"maxFailRatePct\":0}",
                Severity.ERROR);

        seed("Phone digits only", "phone", ValidationType.REGEX_MATCH,
                "{\"pattern\":\"^[+\\\\d\\\\s()-]{7,20}$\",\"maxFailRatePct\":5}",
                Severity.WARNING);

        seed("Name not empty", "name", ValidationType.NOT_EMPTY,
                "{\"maxNullRatePct\":0}",
                Severity.ERROR);

        seed("Age range", "age", ValidationType.NUMERIC_RANGE,
                "{\"min\":0,\"max\":150,\"maxFailRatePct\":0}",
                Severity.ERROR);

        seed("Gender allowed values", "gender", ValidationType.ALLOWED_VALUES,
                "{\"values\":[\"Male\",\"Female\",\"Other\",\"Unknown\"],\"maxFailRatePct\":0}",
                Severity.WARNING);

        seed("Date of birth format", "dob", ValidationType.REGEX_MATCH,
                "{\"pattern\":\"^\\\\d{4}-\\\\d{2}-\\\\d{2}$\",\"maxFailRatePct\":0}",
                Severity.WARNING);

        seed("Global null check (all)", "*", ValidationType.NOT_NULL,
                "{\"maxNullRatePct\":20}",
                Severity.WARNING);

        seed("Unique ID", "id", ValidationType.UNIQUE,
                "{\"maxDuplicateRatePct\":0}",
                Severity.ERROR);

        log.info("Seeded 8 template validation rules");
    }

    private void seed(String name, String columnName, ValidationType type, String params, Severity severity) {
        ValidationRule rule = new ValidationRule();
        rule.setName(name);
        rule.setColumnName(columnName);
        rule.setValidationType(type);
        rule.setParams(params);
        rule.setSeverity(severity);
        rule.setActive(false);
        ruleRepository.save(rule);
    }
}

