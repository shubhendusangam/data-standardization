# Contributing to Data Standardization Pipeline

Thank you for considering a contribution! This guide covers the most common extension
point — **adding a new rule type** — end-to-end, along with local setup, testing, and
PR conventions.

---

## Table of Contents

1. [How to Add a New Rule Type (Worked Example)](#1-how-to-add-a-new-rule-type--worked-example)
2. [Rule Type Contract](#2-rule-type-contract)
3. [Project Setup for Local Development](#3-project-setup-for-local-development)
4. [Running Tests](#4-running-tests)
5. [Pull Request Checklist](#5-pull-request-checklist)
6. [Future Enhancements — Prioritised Roadmap](#6-future-enhancements--prioritised-roadmap)

---

## 1. How to Add a New Rule Type — Worked Example

This walkthrough adds a **`CONCAT`** rule that prepends a prefix and/or appends a suffix
to a field value. By the end you will have touched **five files across two services** plus
the README.

### Step 1 — Create the applier class

Create a new file at:

```
standardization-service/src/main/java/com/datastd/standardization/service/rules/impl/ConcatApplier.java
```

```java
package com.datastd.standardization.service.rules.impl;

import com.datastd.standardization.service.rules.RuleApplier;

import java.util.Map;

/**
 * Concatenates a configurable prefix and/or suffix around the value.
 * Config: {"prefix": "MR-", "suffix": "-v1"}
 * Both keys are optional — omit either to skip that side.
 */
public class ConcatApplier implements RuleApplier {

    @Override
    public String apply(String value, Map<String, Object> config) {
        if (value == null) return value;
        if (config == null) return value;

        String prefix = config.get("prefix") != null ? String.valueOf(config.get("prefix")) : "";
        String suffix = config.get("suffix") != null ? String.valueOf(config.get("suffix")) : "";

        return prefix + value + suffix;
    }
}
```

> **Key point:** the class implements `RuleApplier` and its single method
> `String apply(String value, Map<String, Object> config)`.
> It does **not** use `@Component` — registration happens in the factory (Step 2).

### Step 2 — Register in `RuleApplierFactory`

Open:

```
standardization-service/src/main/java/com/datastd/standardization/service/rules/RuleApplierFactory.java
```

Add a new entry in the constructor:

```java
public RuleApplierFactory() {
    appliers.put("TRIM",          new TrimApplier());
    appliers.put("UPPERCASE",     new UppercaseApplier());
    appliers.put("LOWERCASE",     new LowercaseApplier());
    appliers.put("REPLACE",       new ReplaceApplier());
    appliers.put("MAP_VALUES",    new MapValuesApplier());
    appliers.put("REGEX",         new RegexApplier());
    appliers.put("DEFAULT_VALUE", new DefaultValueApplier());
    appliers.put("DATE_FORMAT",   new DateFormatApplier());
    appliers.put("CONCAT",        new ConcatApplier());   // ← add this
}
```

### Step 3 — Add the enum value to `RuleType`

Open:

```
rule-engine-service/src/main/java/com/datastd/rules/entity/StandardizationRule.java
```

Add `CONCAT` to the `RuleType` enum:

```java
public enum RuleType {
    TRIM,
    UPPERCASE,
    LOWERCASE,
    REPLACE,
    MAP_VALUES,
    REGEX,
    DEFAULT_VALUE,
    DATE_FORMAT,
    CONCAT          // ← add this
}
```

### Step 4 — Add to `RuleConfigValidator`

Open:

```
rule-engine-service/src/main/java/com/datastd/rules/validation/RuleConfigValidator.java
```

Add a case for `CONCAT` in the first `switch` block (the no-config-required group) so
that `prefix` and `suffix` are treated as optional:

```java
switch (ruleType) {
    case TRIM, UPPERCASE, LOWERCASE -> {
        // No config required
        return;
    }
    case CONCAT -> {
        // prefix and suffix are both optional — no required keys
        return;
    }
    default -> {
        // All other types require a non-empty JSON config
    }
}
```

> If your new rule type has **required** config keys, add it to the *second* switch
> instead, using `requireKeys(parsed, typeName, "yourKey")`.

### Step 5 — Update the README rule types table

Add a row to the rule types table in `README.md` so users know `CONCAT` exists and
what config it expects.

### Step 6 — Write tests

Create:

```
standardization-service/src/test/java/com/datastd/standardization/service/rules/impl/ConcatApplierTest.java
```

```java
package com.datastd.standardization.service.rules.impl;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConcatApplierTest {

    private final ConcatApplier applier = new ConcatApplier();

    @Test
    void nullValue_shouldReturnNull() {
        assertThat(applier.apply(null, Map.of("prefix", "A"))).isNull();
    }

    @Test
    void nullConfig_shouldReturnOriginal() {
        assertThat(applier.apply("hello", null)).isEqualTo("hello");
    }

    @Test
    void emptyString_shouldApplyPrefixAndSuffix() {
        Map<String, Object> config = Map.of("prefix", "X-", "suffix", "-Y");
        assertThat(applier.apply("", config)).isEqualTo("X--Y");
    }

    @Test
    void prefixOnly_shouldPrepend() {
        Map<String, Object> config = Map.of("prefix", "MR-");
        assertThat(applier.apply("Smith", config)).isEqualTo("MR-Smith");
    }

    @Test
    void suffixOnly_shouldAppend() {
        Map<String, Object> config = Map.of("suffix", "-v1");
        assertThat(applier.apply("doc", config)).isEqualTo("doc-v1");
    }

    @Test
    void prefixAndSuffix_shouldWrapValue() {
        Map<String, Object> config = Map.of("prefix", "[", "suffix", "]");
        assertThat(applier.apply("data", config)).isEqualTo("[data]");
    }
}
```

Also add factory-level tests in `RuleApplierTest.java` to verify `"CONCAT"` is recognised:

```java
assertThat(factory.getApplier("CONCAT")).isInstanceOf(ConcatApplier.class);
```

### Step 7 — Open a PR

Use the [pull request template](#5-pull-request-checklist) and verify every checkbox
before requesting review.

---

## 2. Rule Type Contract

Every `RuleApplier` implementation **must** guarantee the following:

| Rule | Detail |
|------|--------|
| **Never return `null` from `apply()` unless the input value was `null`** | If transformation is not possible, return the original `value` unchanged. |
| **Throw `RuleApplicationException` on unrecoverable errors** | Located at `com.datastd.standardization.exception.RuleApplicationException`. Never throw raw `NullPointerException`, `IllegalArgumentException`, etc. — wrap them. The `RuleExecutionEngine` catches `RuleApplicationException` and records the error without aborting the job. |
| **Be stateless** | No mutable instance fields. `apply()` must rely only on its arguments. |
| **Be thread-safe** | `apply()` may be called concurrently from the async job thread pool (`AsyncJobProcessor`). Statelessness guarantees this. |
| **Use the canonical type string** | The string registered in `RuleApplierFactory` (e.g. `"CONCAT"`) must match exactly: ① the `RuleType` enum value in `StandardizationRule.java`, ② the `ruleType` sent via `POST /api/rules { "ruleType": "CONCAT" }`, and ③ any entry in `RuleConfigValidator`. |

### Where does each piece live?

| Concern | File |
|---------|------|
| `RuleApplier` interface | `standardization-service/src/main/java/com/datastd/standardization/service/rules/RuleApplier.java` |
| Existing implementations | `standardization-service/src/main/java/com/datastd/standardization/service/rules/impl/` |
| Factory / registry | `standardization-service/src/main/java/com/datastd/standardization/service/rules/RuleApplierFactory.java` |
| Rule execution engine | `standardization-service/src/main/java/com/datastd/standardization/service/rules/RuleExecutionEngine.java` |
| `RuleApplicationException` | `standardization-service/src/main/java/com/datastd/standardization/exception/RuleApplicationException.java` |
| `RuleType` enum | `rule-engine-service/src/main/java/com/datastd/rules/entity/StandardizationRule.java` |
| Config validator | `rule-engine-service/src/main/java/com/datastd/rules/validation/RuleConfigValidator.java` |
| Existing tests | `standardization-service/src/test/java/com/datastd/standardization/service/rules/RuleApplierTest.java` |

---

## 3. Project Setup for Local Development

### Prerequisites

| Tool | Version |
|------|---------|
| Java | 17+ |
| Maven | 3.8+ |
| Docker & Docker Compose | Latest stable |

### Build

```bash
mvn clean package -DskipTests
```

### Run the full stack with Docker

```bash
docker-compose up --build
```

This starts all microservices, Eureka, the API Gateway, Grafana, and Loki.

### Run services locally (without Docker)

Start services in this order:

1. **Eureka Server** (`:8761`) — must be up first
2. **API Gateway** (`:8080`)
3. **Data Ingestion Service** (`:8081`)
4. **Rule Engine Service** (`:8082`)
5. **Standardization Service** (`:8083`)
6. **Data Quality Service** (`:8085`)

Each service can be started with:

```bash
cd <service-directory>
mvn spring-boot:run
```

> See the [README](README.md#quick-start) for detailed quick-start instructions.

### UI

Open any of the HTML files directly in your browser (they fetch from the gateway at
`http://localhost:8080`):

- `dashboard.html` — Pipeline health overview
- `ingestion.html` — Data upload (CSV / Excel / JSON)
- `rules.html` — Rule builder & manager
- `jobs.html` — Job submission & tracking
- `data-quality.html` — Quality dashboard
- `logs.html` — Live log viewer

---

## 4. Running Tests

### Unit tests

```bash
mvn test
```

### Integration tests

Integration tests require Docker (H2 databases start embedded, but some tests exercise
the full Spring context):

```bash
mvn verify -P integration-test
```

### Test coverage

```bash
mvn jacoco:report
```

Reports are generated per module. Open:

```
<module>/target/site/jacoco/index.html
```

For example:

```
standardization-service/target/site/jacoco/index.html
```

---

## 5. Pull Request Checklist

Every PR should use the template at
[`.github/pull_request_template.md`](.github/pull_request_template.md). The key items:

- [ ] Unit tests added / updated
- [ ] `RuleConfigValidator` updated if new `ruleType` added
- [ ] `RuleApplierFactory` registration added if new `ruleType` added
- [ ] `RuleType` enum updated if new `ruleType` added
- [ ] README rule types table updated if new `ruleType` added
- [ ] No raw exceptions — only `RuleApplicationException` thrown from `apply()`
- [ ] `mvn test` passes locally

---

## 6. Future Enhancements — Prioritised Roadmap

| Priority | Enhancement | Status | Notes |
|----------|-------------|--------|-------|
| P0 | PostgreSQL persistent storage | Not started | Blocks production use — replaces H2 |
| P0 | Spring Security + JWT auth | Placeholder ready | See [`SecurityPlaceholder.java`](api-gateway/src/main/java/com/datastd/gateway/config/SecurityPlaceholder.java) |
| P1 | Apache Kafka job queue | Not started | Replace `@Async` thread pool; migration path in README |
| P1 | Source connectors (JDBC, REST, S3) | Not started | See issues |
| P2 | Data quality validation gates | Not started | Block jobs when quality score is below threshold |
| P2 | Rate limiting at gateway | Not started | Spring Cloud Gateway filter |
| P2 | WebSocket live updates | Not started | Push job progress instead of polling |
| P3 | Zipkin trace waterfall | Not started | Add `zipkin-reporter-brave` dependency |
| P3 | UI dark / light theme toggle | Not started | CSS custom properties already in place |
| P3 | Scheduled validation | Not started | Cron-based quality checks with alert notifications |
| P3 | Custom SQL validation | Not started | `CUSTOM_SQL` validation type for advanced checks |

---

## Code of Conduct

Be kind, be constructive, and keep discussions focused on the code.

Happy hacking! 🚀

