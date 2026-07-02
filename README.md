# FX Service

Spring Boot microservice for serving EUR foreign exchange rates.

AI assistance is documented in [AI_USAGE.md](./AI_USAGE.md).

## Challenge Requirements

The service exposes foreign exchange rate data based on the public time series provided by the German central bank:

https://www.bundesbank.de/dynamic/action/en/statistics/time-series-databases/time-series-databases/759784/759784?statisticType=BBK_ITS&listId=www_sdks_b01012_3&treeAnchor=WECHSELKURSE

Functional requirements:

- As a client, I want to get a list of all available currencies.
- As a client, I want to get all EUR-FX exchange rates at all available dates as a collection.
- As a client, I want to get the EUR-FX exchange rate at a particular day.
- As a client, I want to get a foreign exchange amount for a given currency converted to EUR on a particular day.

Technical constraints:

- Java 21
- Spring Boot 3.2.2
- Maven 3.x
- H2 database

System qualities considered during implementation:

- Extensibility
- Testability
- Maintainability
- Low latency
- Reliability and fault tolerance

## Architecture Overview

The service is modeled around two main domain concepts:

- Currency [code(pk), name]
- ExchangeRate [id(pk), currencyCode(fk), date, rate]; unique(currencyCode,date)

The intended serving model is to keep exchange rate data locally in H2 and serve API requests from the local persistence layer. This keeps read paths simple and fast, while isolating Bundesbank-specific integration concerns from the HTTP API and domain logic.

The Java packages follow those component boundaries: the core `rate` package contains the API, service, entity, repository, and DTO classes for serving exchange-rate data, while `rate.importer`, `rate.bundesbank`, and `rate.health` isolate import orchestration, external-provider communication, and operational health reporting.

The architecture is documented as a C4 component diagram in [docs/C4-component-diagram.puml](./docs/C4-component-diagram.puml). It can be visualized with PlantUML-compatible tools such as PlantText.

Architecture decision records are kept in [docs/adr](./docs/adr).

## API

### List all available currencies:

```http
GET /api/v1/currency
```

Response example:

```json
[
  {
    "code": "USD",
    "name": "US Dollar"
  },
  {
    "code": "CHF",
    "name": "Swiss Franc"
  }
]
```

### List all available exchange rates:

```http
GET /api/v1/rate
```

Response example:

```json
[
  {
    "currency": "USD",
    "date": "2026-06-30",
    "rate": 0.9563
  }
]
```

### Get the exchange rate for a currency on a particular date:

```http
GET /api/v1/rate?currency=USD&date=2026-06-30
```

Response example:

```json
1.0956
```

Unsupported query combinations:

- `GET /api/v1/rate?currency=USD`
- `GET /api/v1/rate?date=2026-06-30`

### Getting converted foreign exchange amount

```http
GET /api/v1/convert?currency=USD&date=2026-06-30&amount=100
```

Response example:

```json
94.24
```

## How To Run

Run the test suite:

```shell
./mvnw test
```

On Windows PowerShell:

```powershell
.\mvnw.cmd test
```

Start the application:

```shell
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

By default, the service starts on:

```text
http://localhost:8080
```

## Production-readiness notes

This implementation intentionally keeps the solution simple for the coding challenge. 
A production-ready version would require additional operational and architectural improvements.

### Data import reliability
- Retry with exponential backoff for failed Bundesbank imports.
- Tracking import status, start time, finish time, failure reason, and last successful import timestamp.
- Alerts when exchange-rate data is missing or stale.
- Support of manual re-import for operational recovery.
- Validation of imported data before replacing or publishing it.

### Scalability
Possible solutions for the concurrent data import problem
- distributed locking
- Move import execution to a dedicated scheduled worker (recommended)

### Observability
- Exposing technical metrics through Micrometer/Actuator.
- Tracking business metrics such as number of imported currencies, number of imported rates, etc.
- Adding structured JSON logging for centralized log aggregation.
- Adding distributed tracing with OpenTelemetry.
- Adding dashboards and alerts for failed imports, stale data, high latency, and API error rates.

### Security
- Protecting public APIs with authentication and authorization 
- Restricting Actuator endpoints and exposing them only to internal infrastructure.
- Adding rate limiting for public API usage.
- Using mTLS for internal service to service communication.

### Deployment
- Packaging the service as a container image.
- Replacing H2 with a production database such as PostgreSQL

### Resilience
- Adding timeouts for Bundesbank calls.
- Adding circuit breaker and retry policies for external communication.

### Performance
- using gRPC for internal service to service communication.

### API and documentation
- Adding OpenAPI/Swagger documentation.

