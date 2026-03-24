# Data Standardization Pipeline

A microservices-based system for processing and standardizing data from multiple sources using user-defined JSON rules.

## Architecture

```
┌──────────────┐
│   Clients    │
│ (UI/API/CLI) │
└──────┬───────┘
       │  X-Trace-Id response header
┌──────▼───────┐
│  API Gateway │ :8080
│ (Spring GW)  │─── traceId generated here ───┐
└──────┬───────┘                               │
       │  W3C / B3 trace propagation           │
┌──────▼───────────────────────────────────────▼──┐
│              Eureka Service Discovery  :8761     │
└─────────────────────────────────────────────────┘
       │                │                │
┌──────▼──────┐  ┌──────▼──────┐  ┌─────▼──────────┐
│  Ingestion  │  │ Rule Engine │  │Standardization │
│  Service    │  │  Service    │  │   Service      │
│  :8081      │  │  :8082      │  │   :8083        │
│  (H2 DB)   │  │  (H2 DB)   │  │   (H2 DB)     │
└──────┬──────┘  └──────┬──────┘  └──────┬─────────┘
       │                │                │
       └────────────────┼────────────────┘
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

| Module                      | Port | Description                              |
|-----------------------------|------|------------------------------------------|
| `common-dto`                | —    | Shared DTOs for inter-service Feign APIs |
| `eureka-server`             | 8761 | Service Discovery                        |
| `api-gateway`               | 8080 | API Gateway, routing, request logging    |
| `data-ingestion-service`    | 8081 | Multi-source data ingestion (CSV/Excel/JSON) |
| `rule-engine-service`       | 8082 | Standardization rule & rule-set management |
| `standardization-service`   | 8083 | Async data processing & rule application |

## Project Structure

```
data-standardization/
├── pom.xml                          # Parent POM (dependency management)
├── docker-compose.yml               # Full stack: services + Loki + Grafana
├── common-dto/                      # Shared DTOs (IngestedDatasetResponse, RuleResponse, …)
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
```

> **Note:** Running locally uses the `default` Spring profile — logs output to the console
> with colored formatting and `[traceId, spanId]` in each line. Loki is not required.

### Run with Docker Compose (recommended)

```bash
docker-compose up --build
```

This starts the full stack:
- **5 application services** (Eureka, Gateway, Ingestion, Rules, Standardization)
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

## Supported Rule Types

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
- **Feign calls** — Request/response logging with traceId propagation
- **Errors** — Full stack traces with traceId for cross-service correlation
- **Warnings** — `Dataset not found: id=abc-123`, `Validation failed: {name=required}`

### X-Trace-Id Response Header

Every HTTP response from any service includes an `X-Trace-Id` header:

```bash
curl -v http://localhost:8080/api/ingestion/datasets
# < X-Trace-Id: 64a]f38b0e1d2c3a4b5c6d7e8f901234
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

## H2 Console Access

Each service exposes an H2 console for development:

| Service                  | URL                              | JDBC URL                       |
|--------------------------|----------------------------------|--------------------------------|
| Data Ingestion Service   | http://localhost:8081/h2-console | `jdbc:h2:mem:ingestiondb`      |
| Rule Engine Service      | http://localhost:8082/h2-console | `jdbc:h2:mem:rulesdb`          |
| Standardization Service  | http://localhost:8083/h2-console | `jdbc:h2:mem:standardizationdb`|

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

### Shared DTOs (`common-dto`)

The `common-dto` module contains plain POJOs (`IngestedDatasetResponse`, `RuleResponse`, `RuleSetResponse`)
shared between services via Feign clients. Entity → DTO conversion lives in service-local mapper classes
(`DatasetMapper`, `RuleMapper`) to avoid coupling `common-dto` to JPA entities.

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
- **UI Dashboard** — React/Angular frontend for visual rule configuration and data preview
