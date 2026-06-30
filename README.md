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
    "code": "EUR",
    "name": "Euro"
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
    "currency": "EUR",
    "date": "30-06-2026",
    "rate": 1.0956
  }
]
```

### Get the exchange rate for a currency on a particular date:

```http
GET /api/v1/rate?currency=EUR&date=30-06-2026
```

Response example:

```json
1.0956
```

Unsupported query combinations:

- `GET /api/v1/rate?currency=EUR`
- `GET /api/v1/rate?date=30-06-2026`

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
