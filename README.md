# Data Standardization Pipeline

A microservices-based system for processing, standardizing, and validating data from multiple sources using user-defined JSON rules. Includes a full vanilla HTML/CSS/JS dashboard UI with data quality monitoring, trend analysis, and webhook alerting.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Browser UI (vanilla HTML/CSS/JS)                       │
│  dashboard │ ingestion │ rules │ jobs │ data-quality │ logs                  │
└──────┬────────────┬──────────────┬──────────────┬───────────────────────────┘
       │ fetch()     │ fetch()       │ fetch()       │ fetch()
       │ :8080       │ :8080         │ :8080         │ :3100 (Loki)
┌──────▼────────────▼──────────────▼──────────────▼───┐
│              API Gateway  :8080                      │
│              (Spring Cloud Gateway)                  │
│              X-Trace-Id response header              │
└──────┬───────────┬───────────┬───────────┬──────────┘
       │  W3C / B3  │           │           │
┌──────▼──────────▼───────────▼───────────▼───────────┐
│              Eureka Service Discovery  :8761         │
└─────────────────────────────────────────────────────┘
       │              │              │              │
┌──────▼──────┐ ┌─────▼──────┐ ┌────▼──────────┐ ┌▼──────────────┐
│  Ingestion  │ │Rule Engine │ │Standardization│ │ Data Quality  │
│  Service    │ │  Service   │ │   Service     │ │   Service     │
│  :8081      │ │  :8082     │ │   :8083       │ │   :8085       │
│  (H2 DB)   │ │  (H2 DB)   │ │   (H2 DB)    │ │   (H2 DB)     │
└──────┬──────┘ └─────┬──────┘ └────┬──────────┘ └┬──────────────┘
       │              │              │              │
       └──────────────┼──────────────┼──────────────┘
                      │  Loki4j log shipping
             ┌────────▼────────┐
             │  Grafana Loki   │ :3100
             │  (Log Storage)  │
             └────────┬────────┘
                      │
             ┌────────▼────────┐
             │    Grafana      │ :3000
             │  (Dashboards)   │
             └─────────────────┘
```

## Technology Stack

| Component              | Technology                          |
|------------------------|-------------------------------------|
| Language               | Java 17                             |
| Framework              | Spring Boot 3.2.x                   |
| **UI**                 | **Vanilla HTML / CSS / JS (no framework)** |
| API Gateway            | Spring Cloud Gateway                |
| Service Discovery      | Netflix Eureka                      |
| Inter-service Calls    | OpenFeign                           |
| Distributed Tracing    | Micrometer Tracing + Brave          |
| Structured Logging     | Logback + Logstash JSON Encoder     |
| Log Aggregation        | Grafana Loki + Loki4j Appender      |
| Observability UI       | Grafana (pre-provisioned dashboard) |
| Database               | H2 (dev)                            |
| Build Tool             | Maven (multi-module)                |
| Containerization       | Docker & Docker Compose             |

## Modules

| Module                      | Port | Description                                                   |
|-----------------------------|------|---------------------------------------------------------------|
| `common-dto`                | —    | Shared DTOs for inter-service Feign APIs                      |
| `eureka-server`             | 8761 | Service Discovery                                             |
| `api-gateway`               | 8080 | API Gateway, routing, request logging                         |
| `data-ingestion-service`    | 8081 | Multi-source data ingestion (CSV/Excel/JSON)                  |
| `rule-engine-service`       | 8082 | Standardization rule & rule-set management                    |
| `standardization-service`   | 8083 | Async data processing & rule application                      |
| `data-quality-service`      | 8085 | Data validation, quality scoring, trends & webhook alerts     |

## Project Structure

```
data-standardization/
├── pom.xml                          # Parent POM (dependency management)
├── docker-compose.yml               # Full stack: services + Loki + Grafana
│
├── ── UI (vanilla HTML/CSS/JS) ─────────────────────────────────────
├── dashboard.html                   # Pipeline health overview & KPI tiles
├── ingestion.html                   # Multi-source data upload (file/JSON/manual)
├── rules.html                       # Rule builder & manager + test drawer
├── jobs.html                        # Job submission, tracking & export
├── data-quality.html                # Quality dashboard — scores, trends, alerts
├── logs.html                        # Filtered live log viewer (Loki + fallback)
├── preview-panel.js                 # Reusable before/after diff component
│
├── ── Backend services ─────────────────────────────────────────────
├── common-dto/                      # Shared DTOs (IngestedDatasetResponse, QualityReport, …)
├── eureka-server/                   # Netflix Eureka service registry
├── api-gateway/                     # Spring Cloud Gateway
│   └── config/
│       ├── CorsConfig               # CORS for browser clients
│       ├── RequestLoggingFilter      # Global gateway request/response logging
│       ├── TraceIdResponseFilter     # Injects X-Trace-Id into responses
│       └── SecurityPlaceholder       # JWT auth blueprint (v2)
├── data-ingestion-service/          # Data ingestion
│   ├── controller/IngestionController
│   ├── service/IngestionService      # JSON, CSV, Excel ingestion
│   ├── service/FileParserService     # Apache POI + OpenCSV parsing
│   ├── config/RequestLoggingInterceptor
│   └── config/TraceIdResponseHeaderFilter
├── rule-engine-service/             # Rule management
│   ├── controller/RuleController     # CRUD rules + rule sets + /by-ids
│   ├── service/RuleService
│   ├── config/RequestLoggingInterceptor
│   └── config/TraceIdResponseHeaderFilter
├── standardization-service/         # Data processing
│   ├── controller/StandardizationController
│   ├── service/StandardizationService
│   ├── service/AsyncJobProcessor     # @Async job execution
│   ├── service/rules/                # Rule applier strategy pattern
│   │   ├── RuleApplier (interface)
│   │   ├── RuleApplierFactory
│   │   ├── RuleExecutionEngine
│   │   └── impl/ (Trim, Upper, Lower, Replace, MapValues, Regex, Default, DateFormat)
│   ├── client/IngestionServiceClient # Feign → ingestion-service
│   ├── client/RuleEngineClient       # Feign → rule-engine-service
│   ├── config/AsyncConfig            # Thread pool for job processing
│   ├── config/FeignLoggingConfig     # Feign BASIC logging with traceId
│   ├── config/RequestLoggingInterceptor
│   └── config/TraceIdResponseHeaderFilter
├── data-quality-service/            # Data quality & validation
│   ├── controller/QualityController  # REST API for rules, validation, reports, alerts
│   ├── service/QualityService        # Orchestrates validation, reports, trends, alerts
│   ├── service/QualityAlertService   # Async webhook delivery with HMAC signing & retry
│   ├── engine/
│   │   ├── ValidationEngine          # Core rule evaluator — scoring, duplicate detection
│   │   ├── ColumnProfiler            # Column-level stats — null rate, unique rate, type inference
│   │   ├── RuleSuggestionEngine      # Heuristic rule suggestions from column profiles
│   │   └── TrendEngine               # Linear regression trend (IMPROVING / STABLE / DEGRADING)
│   ├── entity/
│   │   ├── ValidationRule            # Validation rule entity (type, severity, params)
│   │   ├── ValidationRuleSet         # Named group of rule IDs
│   │   ├── QualityReportEntity       # Persisted report (score, status, JSON blob)
│   │   ├── QualityAlertConfig        # Webhook alert configuration
│   │   ├── ValidationType            # Enum: NOT_NULL, REGEX_MATCH, UNIQUE, …
│   │   └── Severity                  # Enum: ERROR, WARNING
│   ├── client/IngestionServiceClient # Feign → ingestion-service
│   ├── mapper/QualityMapper          # Entity → DTO conversion
│   └── exception/GlobalExceptionHandler
└── infra/
    ├── loki/loki-config.yml          # Loki server configuration
    └── grafana/provisioning/
        ├── datasources/loki.yml      # Auto-provision Loki datasource
        └── dashboards/
            ├── dashboards.yml        # Dashboard provisioning config
            └── json/centralized-logs.json  # Pre-built dashboard
```

## Quick Start

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose (for full stack including observability)

### Run Locally (without Docker)

Start services in this order:

```bash
# 1. Build all modules
mvn clean package -DskipTests

# 2. Start Eureka Server
cd eureka-server && mvn spring-boot:run &

# 3. Start API Gateway
cd api-gateway && mvn spring-boot:run &

# 4. Start Data Ingestion Service
cd data-ingestion-service && mvn spring-boot:run &

# 5. Start Rule Engine Service
cd rule-engine-service && mvn spring-boot:run &

# 6. Start Standardization Service
cd standardization-service && mvn spring-boot:run &

# 7. Start Data Quality Service
cd data-quality-service && mvn spring-boot:run &
```

> **Note:** Running locally uses the `default` Spring profile — logs output to the console
> with colored formatting and `[traceId, spanId]` in each line. Loki is not required.

### Run with Docker Compose (recommended)

```bash
docker-compose up --build
```

This starts the full stack:
- **6 application services** (Eureka, Gateway, Ingestion, Rules, Standardization, Data Quality)
- **Grafana Loki** — centralized log storage
- **Grafana** — pre-provisioned dashboard at `http://localhost:3000`

### Run Tests

```bash
mvn test
```

## API Endpoints

All endpoints are accessible through the API Gateway at `http://localhost:8080`.
Every response includes an **`X-Trace-Id`** header for log correlation.

### Data Ingestion Service (`/api/ingestion`)

| Method   | Endpoint                       | Description              |
|----------|--------------------------------|--------------------------|
| `POST`   | `/api/ingestion/upload`        | Upload Excel/CSV file    |
| `POST`   | `/api/ingestion/json`          | Ingest JSON data         |
| `GET`    | `/api/ingestion/datasets`      | List all datasets        |
| `GET`    | `/api/ingestion/datasets/{id}` | Get dataset by ID        |
| `DELETE` | `/api/ingestion/datasets/{id}` | Delete dataset           |

### Rule Engine Service (`/api/rules`)

| Method   | Endpoint                    | Description                                      |
|----------|-----------------------------|--------------------------------------------------|
| `POST`   | `/api/rules`                | Create a rule                                    |
| `GET`    | `/api/rules`                | List rules (filter: `fieldName`, `ruleType`, `active`) |
| `GET`    | `/api/rules/{id}`           | Get rule by ID                                   |
| `PUT`    | `/api/rules/{id}`           | Update rule                                      |
| `DELETE` | `/api/rules/{id}`           | Delete rule                                      |
| `PATCH`  | `/api/rules/{id}/toggle`    | Toggle rule active status                        |
| `POST`   | `/api/rules/by-ids`         | Fetch rules by list of IDs (used by Feign)       |
| `POST`   | `/api/rules/rulesets`       | Create a rule set                                |
| `GET`    | `/api/rules/rulesets`       | List all rule sets                               |
| `GET`    | `/api/rules/rulesets/{id}`  | Get rule set with rules                          |

### Standardization Service (`/api/standardization`)

| Method | Endpoint                                   | Description              |
|--------|--------------------------------------------|--------------------------|
| `POST` | `/api/standardization/process`             | Submit processing job (returns 202) |
| `GET`  | `/api/standardization/jobs`                | List jobs (filter: `status`, `datasetId`) |
| `GET`  | `/api/standardization/jobs/{jobId}`        | Get job status & progress |
| `GET`  | `/api/standardization/jobs/{jobId}/result` | Get standardized results |
| `POST` | `/api/standardization/preview`             | Preview standardization (sync) |

### Data Quality Service (`/api/quality`)

#### Validation Rules

| Method   | Endpoint                        | Description                                         |
|----------|---------------------------------|-----------------------------------------------------|
| `POST`   | `/api/quality/rules`            | Create a validation rule                            |
| `GET`    | `/api/quality/rules`            | List rules (filter: `columnName`, `validationType`, `active`) |
| `PUT`    | `/api/quality/rules/{id}`       | Update a validation rule                            |
| `DELETE` | `/api/quality/rules/{id}`       | Delete a validation rule                            |
| `PATCH`  | `/api/quality/rules/{id}/toggle`| Toggle rule active status                           |
| `GET`    | `/api/quality/rules/templates`  | List built-in template rules                        |
| `POST`   | `/api/quality/rules/suggest`    | Auto-suggest rules for a dataset (AI heuristics)    |

#### Validation Rule Sets

| Method | Endpoint                        | Description                |
|--------|---------------------------------|----------------------------|
| `POST` | `/api/quality/rulesets`         | Create a validation rule set |
| `GET`  | `/api/quality/rulesets`         | List all rule sets         |
| `GET`  | `/api/quality/rulesets/{id}`    | Get rule set by ID         |

#### Validation Execution

| Method | Endpoint                  | Description                         |
|--------|---------------------------|-------------------------------------|
| `POST` | `/api/quality/validate`   | Run validation and get quality report |

#### Reports

| Method | Endpoint                               | Description                                |
|--------|----------------------------------------|--------------------------------------------|
| `GET`  | `/api/quality/reports/{datasetId}`     | Get latest report for a dataset            |
| `GET`  | `/api/quality/reports/{reportId}/full` | Get full report by report ID               |
| `GET`  | `/api/quality/reports`                 | List reports (filter: `datasetId`, `overallStatus`) |
| `GET`  | `/api/quality/reports/history`         | Paginated report history (`datasetId`, `page`, `size`) |

#### Trend & Summary

| Method | Endpoint                                     | Description                              |
|--------|----------------------------------------------|------------------------------------------|
| `GET`  | `/api/quality/datasets/{datasetId}/trend`    | Quality trend over time (`days` param, default 30) |
| `GET`  | `/api/quality/summary`                       | All dataset quality summaries (filter: `status`) |

#### Webhook Alerts

| Method   | Endpoint                    | Description                |
|----------|-----------------------------|----------------------------|
| `POST`   | `/api/quality/alerts`       | Create alert configuration |
| `GET`    | `/api/quality/alerts`       | List all alert configs     |
| `PUT`    | `/api/quality/alerts/{id}`  | Update alert configuration |
| `DELETE` | `/api/quality/alerts/{id}`  | Delete alert configuration |

## Supported Standardization Rule Types

| Rule Type       | Config Example                                                  | Description                        |
|-----------------|----------------------------------------------------------------|------------------------------------|
| `TRIM`          | `{}`                                                           | Trim whitespace                    |
| `UPPERCASE`     | `{}`                                                           | Convert to uppercase               |
| `LOWERCASE`     | `{}`                                                           | Convert to lowercase               |
| `REPLACE`       | `{"find": "old", "replace": "new"}`                           | Find and replace text              |
| `MAP_VALUES`    | `{"mappings": {"M": "Male", "F": "Female"}}`                  | Map values to standardized values  |
| `REGEX`         | `{"pattern": "[^0-9]", "replacement": ""}`                    | Regex-based replacement            |
| `DEFAULT_VALUE` | `{"defaultValue": "N/A"}`                                     | Set default for null/empty fields  |
| `DATE_FORMAT`   | `{"sourceFormat": "MM/dd/yyyy", "targetFormat": "yyyy-MM-dd"}`| Format dates                       |

## Supported Validation Types (Data Quality)

| Validation Type  | Severity   | Params Example                                                 | Description                           |
|------------------|------------|----------------------------------------------------------------|---------------------------------------|
| `NOT_NULL`       | ERROR/WARN | `{"maxNullRatePct": 10.0}`                                     | Null rate must be below threshold     |
| `NOT_EMPTY`      | ERROR/WARN | `{"maxNullRatePct": 0.0}`                                      | Blank/null rate below threshold       |
| `REGEX_MATCH`    | ERROR/WARN | `{"pattern": "^[\\w.+]+@[\\w.]+\\.[a-z]{2,}$", "maxFailRatePct": 0.0}` | All values must match pattern |
| `ALLOWED_VALUES` | ERROR/WARN | `{"values": ["Male","Female","Other"], "maxFailRatePct": 0.0}` | Values must be in allowed set         |
| `NUMERIC_RANGE`  | ERROR/WARN | `{"min": 0, "max": 150, "maxFailRatePct": 0.0}`               | Numeric values within [min, max]      |
| `MIN_LENGTH`     | ERROR/WARN | `{"length": 2}`                                                | String length ≥ minimum               |
| `MAX_LENGTH`     | ERROR/WARN | `{"length": 5}`                                                | String length ≤ maximum               |
| `UNIQUE`         | ERROR/WARN | `{"maxDuplicateRatePct": 0.0}`                                 | No duplicate values in column         |

### Quality Scoring

- **Base score**: 100
- **Each failed ERROR rule**: −20 points
- **Each failed WARNING rule**: −5 points
- **Floor**: 0 (score never goes negative)
- **Overall status**: `FAIL` if any ERROR rule fails · `WARN` if only WARNING rules fail · `PASS` otherwise
- **Duplicate detection**: SHA-256 row hashing for row-level duplicate counting
- **Column profiling**: null rate, unique rate, min/max, type inference (STRING, INTEGER, DECIMAL, BOOLEAN, DATE, MIXED, NULL)

### Webhook Alerts

After every `POST /api/quality/validate`, the system evaluates all active alert configurations:

- **Trigger on status**: fire when `overallStatus` matches (e.g., `FAIL`, `WARN`)
- **Trigger on score**: fire when `qualityScore < threshold`
- **Delivery**: async HTTP POST to configured webhook URL with HMAC-SHA256 signature
- **Retry**: up to 3 attempts with 5 s delay between retries

## Usage Examples

### 1. Ingest JSON Data

```bash
curl -X POST http://localhost:8080/api/ingestion/json \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Customer Data",
    "records": [
      {"name": "  John Doe  ", "gender": "M", "email": "JOHN@EXAMPLE.COM", "dob": "03/15/1990"},
      {"name": "  Jane Smith  ", "gender": "F", "email": "JANE@EXAMPLE.COM", "dob": "12/25/1985"}
    ]
  }'
```

### 2. Upload a CSV File

```bash
curl -X POST http://localhost:8080/api/ingestion/upload \
  -F "file=@data.csv"
```

### 3. Create Standardization Rules

```bash
# Trim names
curl -X POST http://localhost:8080/api/rules \
  -H "Content-Type: application/json" \
  -d '{"name": "Trim Name", "fieldName": "name", "ruleType": "TRIM", "priority": 1}'

# Standardize gender
curl -X POST http://localhost:8080/api/rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Standardize Gender",
    "fieldName": "gender",
    "ruleType": "MAP_VALUES",
    "ruleConfig": "{\"mappings\": {\"M\": \"Male\", \"F\": \"Female\", \"m\": \"Male\", \"f\": \"Female\"}}",
    "priority": 2
  }'

# Lowercase emails
curl -X POST http://localhost:8080/api/rules \
  -H "Content-Type: application/json" \
  -d '{"name": "Lowercase Email", "fieldName": "email", "ruleType": "LOWERCASE", "priority": 3}'

# Format dates
curl -X POST http://localhost:8080/api/rules \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Format DOB",
    "fieldName": "dob",
    "ruleType": "DATE_FORMAT",
    "ruleConfig": "{\"sourceFormat\": \"MM/dd/yyyy\", \"targetFormat\": \"yyyy-MM-dd\"}",
    "priority": 4
  }'
```

### 4. Create a Rule Set (optional)

```bash
curl -X POST http://localhost:8080/api/rules/rulesets \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Customer Cleanup",
    "description": "Standard cleanup for customer data",
    "ruleIds": ["<RULE_UUID_1>", "<RULE_UUID_2>", "<RULE_UUID_3>", "<RULE_UUID_4>"]
  }'
```

### 5. Process Data

```bash
# Using individual rule IDs:
curl -X POST http://localhost:8080/api/standardization/process \
  -H "Content-Type: application/json" \
  -d '{
    "datasetId": "<DATASET_UUID>",
    "ruleIds": ["<RULE_UUID_1>", "<RULE_UUID_2>", "<RULE_UUID_3>", "<RULE_UUID_4>"]
  }'

# Or using a rule set:
curl -X POST http://localhost:8080/api/standardization/process \
  -H "Content-Type: application/json" \
  -d '{"datasetId": "<DATASET_UUID>", "ruleSetId": "<RULESET_UUID>"}'
```

### 6. Check Job Status & Result

```bash
# Poll for status (returns progress percentage)
curl http://localhost:8080/api/standardization/jobs/<JOB_UUID>

# Get final result
curl http://localhost:8080/api/standardization/jobs/<JOB_UUID>/result
```

### 7. Preview Before Processing

```bash
curl -X POST "http://localhost:8080/api/standardization/preview?maxRecords=5" \
  -H "Content-Type: application/json" \
  -d '{"datasetId": "<DATASET_UUID>", "ruleIds": ["<RULE_UUID_1>", "<RULE_UUID_2>"]}'
```

### 8. Validate Data Quality

```bash
# Run validation against a dataset
curl -X POST http://localhost:8080/api/quality/validate \
  -H "Content-Type: application/json" \
  -d '{"datasetId": "<DATASET_UUID>", "ruleIds": ["<VALIDATION_RULE_UUID_1>"]}'

# Auto-suggest validation rules for a dataset
curl -X POST http://localhost:8080/api/quality/rules/suggest \
  -H "Content-Type: application/json" \
  -d '{"datasetId": "<DATASET_UUID>"}'

# Get quality trend for a dataset (last 30 days)
curl http://localhost:8080/api/quality/datasets/<DATASET_UUID>/trend?days=30

# Get quality summary for all datasets
curl http://localhost:8080/api/quality/summary
```

### 9. Configure Webhook Alerts

```bash
# Create an alert that fires on FAIL or score below 50
curl -X POST http://localhost:8080/api/quality/alerts \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Slack Quality Alert",
    "webhookUrl": "https://hooks.slack.com/services/xxx/yyy/zzz",
    "triggerOnStatus": ["FAIL"],
    "triggerOnScoreBelow": 50,
    "active": true,
    "secret": "my-hmac-secret"
  }'
```

## UI Dashboard

A complete browser-based management interface built with **vanilla HTML, CSS, and JS** — zero
frameworks, zero build step. All API calls go through the gateway at `:8080`. CSS custom
properties in `:root` enable full theming. Open any `.html` file directly in a browser.

### Screens

| File               | Purpose                                                                |
|--------------------|------------------------------------------------------------------------|
| `dashboard.html`   | Pipeline health overview — KPI tiles, topology, active jobs, throughput chart, event log |
| `ingestion.html`   | Multi-source data upload — file drag-and-drop, JSON input, manual table editor |
| `rules.html`       | Rule builder & manager — two-column layout, drag-reorder, test drawer  |
| `jobs.html`        | Job submission, tracking & export — table, detail drawer, new-job modal |
| `data-quality.html`| Quality dashboard — scores, validation reports, trends, rule suggestions, alerts |
| `logs.html`        | Filtered live log viewer — Loki primary, job-status fallback           |
| `preview-panel.js` | Reusable before/after diff component (used by `rules.html` and `jobs.html`) |

### Dashboard (`dashboard.html`)

- **KPI strip** — 5 tiles: total datasets, active rules, running jobs, records processed, avg job time
- **Pipeline topology** — 6 service nodes with live health-status dots (polls `GET /actuator/health` per service every 5 s)
- **Active jobs** — polls `GET /api/standardization/jobs?status=PROCESSING` every 3 s with progress bars
- **Throughput chart** — CSS-only bar chart of records/hour bucketed from job history
- **Event log** — last 20 events from job status changes, color-coded by level

### Ingestion (`ingestion.html`)

- **Three upload modes** as segmented tabs:
  - **File Upload** — drag-and-drop `.csv` / `.xlsx` zone → `POST /api/ingestion/upload` (multipart)
  - **JSON Input** — textarea with real-time client-side JSON validation → `POST /api/ingestion/json`
  - **Manual Entry** — dynamic column/row table editor → assembles JSON payload on submit
- **Parsed preview table** — auto-inferred column type badges (string, number, boolean, date, mixed, null)
- **Dataset list** — `GET /api/ingestion/datasets` with preview and delete per row

### Rules (`rules.html`)

- **Two-column layout** — left: filterable/drag-reorderable rule list, right: rule builder form
- **Rule list** — active toggle (`PATCH /api/rules/{id}/toggle`), type badge, edit/delete actions
- **Rule builder** — `ruleType` dropdown dynamically renders the correct config sub-form:
  - `REPLACE`: find / replace inputs
  - `MAP_VALUES`: dynamic key → value pair editor
  - `DATE_FORMAT`: source / target format dropdowns
  - `REGEX`: pattern + replacement + **live regex test preview**
  - `TRIM` / `UPPERCASE` / `LOWERCASE`: no config
  - `DEFAULT_VALUE`: single input
- **Rule sets panel** — list existing sets, select rules + create (`POST /api/rules/rulesets`)
- **Test drawer** — enter sample value → client-side rule application → before/after display; optional dataset preview via `POST /api/standardization/preview`

### Jobs (`jobs.html`)

- **Job table** — polls all jobs every 3 s, progress bars, status badges
  - `PROCESSING` badge = amber with CSS pulse animation
  - `COMPLETED` = green, `FAILED` = red, `QUEUED` = blue
- **Row click → detail drawer** — full metadata grid, X-Trace-Id (copyable), result table
  - **COMPLETED**: Export CSV + Export JSON download from `/jobs/{id}/result`
  - **FAILED**: red error message box showing `errorLog`
- **New Job modal** — 2-step wizard:
  1. Select dataset (cards from ingestion service)
  2. Pick individual rules OR a rule set
  - **Preview button**: inline diff panel (via `preview-panel.js`) before submitting

### Data Quality (`data-quality.html`)

- **Quality score cards** — per-dataset quality scores with trend indicators (↑ ↓ →)
- **Validation report** — column-level stats, rule results with pass/fail badges
- **Trend chart** — Chart.js time-series of quality scores over configurable date range
- **Rule suggestions** — auto-generated validation rules from column profiling heuristics
- **Alert configuration** — CRUD for webhook alert configs (status triggers, score thresholds)
- **Report history** — paginated list of past validation runs with drill-down

### Logs (`logs.html`)

- **Filter bar** — service multi-select chips, level chips (INFO / WARN / ERROR / DEBUG), Trace ID text input
- **Log table** — monospace, columns: timestamp / service / level / message
  - INFO = teal, WARN = amber, ERROR = red, DEBUG = muted
- **UUID auto-linking** — regex detection, links job UUIDs to `jobs.html`, dataset UUIDs to `ingestion.html`
- **Auto-scroll toggle** + "Jump to latest" badge when scrolled up
- **Primary source**: Grafana Loki `/loki/api/v1/query_range` (port 3100)
- **Fallback**: polls `GET /api/standardization/jobs` to synthesize log entries when Loki is unavailable

### Preview Panel (`preview-panel.js`)

Reusable vanilla JS component — zero dependencies, CSS injected once at runtime.

```js
PreviewPanel.render(containerEl, {
  original:      [{ name: 'Alice', … }, …],   // original records
  standardized:  [{ name: 'alice', … }, …],   // after rules applied
  changedFields: { name: 'Lowercase Name' },   // field → rule name
  elapsedMs:     42                             // optional timing
});
```

- **Summary strip** — "N of M records changed · K fields · Xms"
- **Two-column diff table** — Original vs. Standardized, synchronized horizontal scroll
- **Changed cells** — amber highlight + hover tooltip showing rule name
- **Export** — CSV and JSON download via Blob API
- **Data source** — `POST /api/standardization/preview?maxRecords=N`

## Centralized Logging & Distributed Tracing

Every request receives a **traceId** at the API Gateway that propagates through all downstream
Feign calls, allowing you to follow a single user request across all services.

### Observability Stack

| Component                  | Role                                                        |
|----------------------------|-------------------------------------------------------------|
| Micrometer Tracing + Brave | Generates `traceId`/`spanId`, propagates via W3C + B3 headers |
| Logstash Logback Encoder   | Structured JSON log output (in `docker` profile)            |
| Loki4j Logback Appender    | Ships logs directly from each JVM to Grafana Loki           |
| Grafana Loki               | Centralized log aggregation & querying                      |
| Grafana                    | Pre-provisioned dashboard for log viewing & trace lookup    |

### Logging Profiles

Controlled by `SPRING_PROFILES_ACTIVE`:

| Profile     | Console Output        | Loki Shipping | Use Case          |
|-------------|----------------------|---------------|--------------------|
| `default`   | Colored + `[traceId,spanId]` | No      | Local development  |
| `docker`    | Structured JSON       | Yes           | Docker Compose     |

**Local dev log format:**
```
14:23:45.123 INFO  [data-ingestion-service] [abc123,def456] c.d.i.service.impl.IngestionServiceImpl : Ingesting JSON data: name=Customer Data, recordCount=2
```

### What Gets Logged

Every service logs structured events at key points:

- **Request logging** — `→ POST /api/ingestion/json from 172.18.0.1` and `← POST /api/ingestion/json → 201 (45ms)`
- **Business operations** — `Ingesting JSON data: name=Customer Data, recordCount=2`
- **Entity lifecycle** — `Dataset persisted: id=abc-123, name=Customer Data, status=PARSED`
- **Job progress** — `Job queued: jobId=xyz, datasetId=abc, ruleCount=4`
- **Quality validation** — `Starting validation: datasetId=abc, records=100, rules=5`
- **Alert delivery** — `Webhook alert sent: config=Slack Alert, reportId=xyz, status=200`
- **Feign calls** — Request/response logging with traceId propagation
- **Errors** — Full stack traces with traceId for cross-service correlation
- **Warnings** — `Dataset not found: id=abc-123`, `Validation failed: {name=required}`

### X-Trace-Id Response Header

Every HTTP response from any service includes an `X-Trace-Id` header:

```bash
curl -v http://localhost:8080/api/ingestion/datasets
# < X-Trace-Id: 64af38b0e1d2c3a4b5c6d7e8f901234
```

Use this value to search logs in Grafana.

### Grafana Dashboard

**URL:** `http://localhost:3000` (user: `admin`, password: `admin`)

The pre-provisioned **"Data Standardization — Centralized Logs & Tracing"** dashboard provides:

| Panel                        | Description                                    |
|------------------------------|------------------------------------------------|
| Log Stream                   | Live logs filtered by service and log level     |
| Error Rate by Service        | Errors per minute per service (timeseries)      |
| Log Volume by Service        | Total log lines per minute per service          |
| Log Level Distribution       | DEBUG/INFO/WARN/ERROR breakdown over time       |
| WARN + ERROR Logs            | Filtered view of only warnings and errors       |
| Trace Lookup                 | Paste a traceId to see all logs from that request |

**Dashboard variables:**
- **Service** — Filter by one or more services (or "All")
- **Log Level** — Filter by DEBUG, INFO, WARN, ERROR (or "All")
- **Trace ID** — Text box for cross-service trace correlation

### Searching by Trace ID (LogQL)

```
{service=~".+"} |= "YOUR_TRACE_ID_HERE"
```

Or filter by service:

```
{service="standardization-service"} | json | traceId = "YOUR_TRACE_ID_HERE"
```

## Eureka Lease Configuration

All client services send heartbeats every **10 seconds** (default: 30 s) with a lease expiration of
**30 seconds** (default: 90 s). The Eureka server runs eviction every **15 seconds** with
self-preservation disabled. This prevents "lease doesn't exist" warnings during development.

| Setting                                    | Value | Location        |
|--------------------------------------------|-------|-----------------|
| `lease-renewal-interval-in-seconds`        | 10    | All clients     |
| `lease-expiration-duration-in-seconds`     | 30    | All clients     |
| `eviction-interval-timer-in-ms`            | 15000 | Eureka server   |
| `expected-client-renewal-interval-seconds` | 10    | Eureka server   |
| `enable-self-preservation`                 | false | Eureka server   |

## H2 Console Access

Each service exposes an H2 console for development:

| Service                  | URL                              | JDBC URL                       |
|--------------------------|----------------------------------|--------------------------------|
| Data Ingestion Service   | http://localhost:8081/h2-console | `jdbc:h2:mem:ingestiondb`      |
| Rule Engine Service      | http://localhost:8082/h2-console | `jdbc:h2:mem:rulesdb`          |
| Standardization Service  | http://localhost:8083/h2-console | `jdbc:h2:mem:standardizationdb`|
| Data Quality Service     | http://localhost:8085/h2-console | `jdbc:h2:mem:qualitydb`        |

Username: `sa` / Password: _(empty)_

## Architecture Decisions

### Async Job Processing

`POST /api/standardization/process` returns **202 Accepted** immediately with a job ID.
Processing runs asynchronously via a dedicated thread pool (`std-job-` threads configured in `AsyncConfig`).
Clients poll `GET /api/standardization/jobs/{jobId}` for status and progress.

**Why a separate `AsyncJobProcessor` bean?** Spring's `@Async` uses AOP proxies — calling
`this.processJobAsync()` within the same class bypasses the proxy and runs synchronously.
Extracting into a separate `@Service` ensures the call goes through the proxy.

**Kafka migration path:** Replace `@Async` + thread pool with Kafka:
1. `submitJob()` publishes to topic `standardization.jobs`
2. `AsyncJobProcessor` becomes a `@KafkaListener` consumer
3. Remove `@EnableAsync` and `AsyncConfig`

### Rule Applier Strategy Pattern

Each rule type is a `RuleApplier` implementation. `RuleApplierFactory` maps `ruleType` strings
to applier instances. `RuleExecutionEngine` iterates the sorted rules and applies them to each
record field. Adding a new rule type requires only a new `RuleApplier` impl + a factory entry.

### Validation Engine Design

The `ValidationEngine` evaluates records against validation rules and produces a `QualityReport`:

1. **Column profiling** — `ColumnProfiler` computes null rate, unique rate, min/max, sample values, and inferred type for every column
2. **Duplicate detection** — SHA-256 row hashing identifies exact-duplicate rows
3. **Rule evaluation** — each active rule is evaluated per-column (wildcard `*` expands to all columns)
4. **Scoring** — penalty-based (−20 per ERROR, −5 per WARNING, floor 0)
5. **Alert dispatch** — async webhook delivery via `QualityAlertService` with HMAC signing and retry

### Rule Suggestion Engine

The `RuleSuggestionEngine` uses column name heuristics and column profiling statistics to generate
validation rule suggestions. For example:
- Column named `email` → suggest `REGEX_MATCH` with email pattern
- Column with high null rate → suggest `NOT_NULL` with appropriate threshold
- Column with low cardinality → suggest `ALLOWED_VALUES` with discovered values
- Numeric column named `age` → suggest `NUMERIC_RANGE` with observed min/max

### Shared DTOs (`common-dto`)

The `common-dto` module contains plain POJOs (`IngestedDatasetResponse`, `RuleResponse`, `RuleSetResponse`,
`QualityReport`, `ColumnReport`, `ValidationRuleResult`) shared between services via Feign clients.
Entity → DTO conversion lives in service-local mapper classes (`DatasetMapper`, `RuleMapper`, `QualityMapper`)
to avoid coupling `common-dto` to JPA entities.

### Security (v2 Placeholder)

No authentication is enabled in v1. When needed:
- Uncomment the security dependencies in `api-gateway/pom.xml`
- Implement the `SecurityWebFilterChain` bean described in `SecurityPlaceholder.java`
- Configure the JWT issuer URI in `api-gateway/application.yml`

## Future Enhancements

- **Apache Kafka** — Event-driven async processing (replace `@Async` thread pool)
- **Elasticsearch / MongoDB** — Persistent storage for datasets and standardized results
- **Spring Security + JWT** — Authentication & authorization (v2 placeholder ready)
- **Rate Limiting** — API throttling at the gateway
- **Zipkin** — Visual trace waterfall diagrams (add `zipkin-reporter-brave` dependency)
- **Prometheus + Grafana** — Metrics monitoring (Spring Boot Actuator metrics endpoint)
- **WebSocket live updates** — Push job progress and log entries instead of polling
- **Dark/light theme toggle** — UI already uses CSS custom properties; add a switcher
- **Quality gates** — Block standardization jobs when data quality score is below threshold
- **Scheduled validation** — Cron-based automatic quality checks with alert notifications
- **Custom SQL validation** — `CUSTOM_SQL` validation type for advanced data checks
