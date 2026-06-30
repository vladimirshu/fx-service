# 0001. Local DB Serving Model

## Status

Accepted

## Context

The service must provide EUR foreign exchange rate data obtained from the Deutsche Bundesbank public service.

The following functionality is required:

* Retrieve a list of available currencies.
* Retrieve all available EUR-FX exchange rates.
* Retrieve an exchange rate for a specific currency and date.
* Convert a foreign currency amount to EUR using the exchange rate applicable on a specific date.

The Bundesbank service is an external dependency whose availability and response times are outside the control of this application.

The challenge requires the use of H2 as the database.

## Decision Drivers

* Low latency for API consumers.
* Reliability and availability independent of Bundesbank availability.
* Maintainability and clear separation of concerns.
* Testability of business logic without relying on external services.
* Extensibility for additional functionality.
* Simplicity appropriate for the scope of the coding challenge.

## Considered Options

### Option 1: Query Bundesbank on every API request

The application acts as a proxy and requests exchange rates from Bundesbank whenever a client calls the API.

Pros:

* No local storage required.
* Always uses the latest available data.

Cons:

* API availability depends on Bundesbank availability.
* Higher response latency.
* Increased complexity in testing.
* Repeated retrieval of the same data.

### Option 2: Load exchange rates into local database and serve requests from the database

The application imports exchange-rate data from Bundesbank and stores it locally. Client requests are served exclusively from the local database.

Pros:

* Fast and predictable response times.
* External provider outages do not immediately affect API consumers.
* Easier testing and development.
* Clear separation between data acquisition and data serving.

Cons:

* Requires import and synchronization logic.
* Data freshness depends on successful imports.

### Option 3: Hybrid approach with database and fallback to Bundesbank

The application primarily uses the local database but falls back to Bundesbank when data is missing.

Pros:

* Potentially higher data availability.

Cons:

* More complex implementation.
* Less predictable behavior.
* Increased testing effort.
* Additional runtime dependency on Bundesbank.

## Decision

Option 2 is selected.

Exchange-rate data will be imported from Bundesbank and stored in a local H2 database.

All public API endpoints will retrieve exchange-rate information exclusively from the local database.

The application will perform an initial import when the database is empty.

A scheduled synchronization job will periodically import newly available exchange-rate data.

Bundesbank integration will be isolated behind a dedicated client component to allow replacement of the provider in the future.

## Consequences

### Positive

* API response times are independent of Bundesbank response times.
* Existing exchange-rate data remains available during Bundesbank outages.
* Business logic can be tested using local fixtures and mocks.
* Clear separation between external integration and application services.
* Additional exchange-rate providers can be introduced with limited impact on the rest of the system.

### Negative

* Additional storage is required.
* Import failures must be detected and handled.
* Exchange-rate data may become stale if synchronization fails.
* Application readiness depends on the presence of imported data.

## Validation

The decision is considered successful if:

* All API endpoints can serve requests without contacting Bundesbank.
* The application remains operational when Bundesbank is temporarily unavailable.
* Exchange-rate data can be imported repeatedly without creating duplicates.
* Tests can be executed without requiring network access to Bundesbank.
* API response times remain consistent regardless of Bundesbank performance.


