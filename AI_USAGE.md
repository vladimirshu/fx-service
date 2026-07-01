AI coding agent: Codex with Intellij Idea

# AI usage strategy
1. Elaborated on requirements, design decisions, and trade-offs.
2. Generated and reviewed a well-structured ADR document based on the long collaboration with LLM. 
3. Beautified documentation.

# Key prompts & responses

## Implementing use case 1: Get all currencies
Let's implement the first requirement in the README file:
- As a client, I want to get a list of all available currencies.

for that:
1. Add currency as ORM entity based on the entity description in the readme file.
2. And required configuration to the application.yaml so the H2 database schema is automatically re-created for the entity on the application startup.
3. Create CurrencyDTO and CurrencyMapper. Implement one-to-one mapping between entity and DTO.
4. Create an exchange rate service that lists all currencies. Method must return list of currencyDTO
5. Create an exchange rate controller with one endpoint that returns all currencies, based on the API description in the README file.
6. Create a test suite for the given use case. It must consist of the two test files: service and controller unit tests. Create a small test dataset consisting of two different currencies.

## Initial currency import
Let's implement currency's import on application startup.
Please reference C4 components model diagram (.puml) description for the architecture overview.

for that:
create exchange rate import job with one method that starts import of the currency exchange data and is executed on application startup. The method must invoke the ExchangeRateImporter class method that mocks response with 2 sample currencies (later it will call BundesbankClient to get real data). For now, skip any logic for the exchange rate entity.

## Getting currency list from Bundesbank
let's implement an API call to the Bundesbank and parse the response so it can be used by application logic

for that:
- create a BundesbankRestClient that gets list of currencies in a json format by url
  GET https://api.statistiken.bundesbank.de/rest/data/BBEX3/D..EUR.BB.AC.000?startPeriod=2026-06-30&endPeriod=2026-06-30&format=sdmx_json
  please update Start and End periods in the url parameters with the current date.
  Expected response is attached.
  Please extract list of the currencies from the response.
- create ImportedCurrency java record and map bundesbank API response to it.
- update ExchangeRateImporter so it uses BundesbankRestClient instead of the sample data

## Daily update of currency exchange data
Let's implement daily update of the currency exchange data

for that:
1. Add another method to the ExchangeRateImportJob that updates currency exchange data and is scheduled to be executed daily, at 12 am.
2. Add another method to the ExchangeRateImporter that will implement the currency exchange data update logic. For that use existing logic to get a list of all currencies from the BundesbankRestClient. Then compare the result with the currencies available in the database. Find delta and update the DB: If there are new currencies available via API, please add them to the database. If some currencies were removed, please also remove them from the database.

## Implementing use case 2: Get all EUR-FX exchange rates
Let's implement the following use case:
As a client, I want to get all EUR-FX exchange rates at all available dates as a collection.

For that:
- create ExchangeRate entity based on the definition in the readme file
- create relevant service method; DTO and mapper classes the same way they exist for the currency.
- create controller endpoint based on the definition in the readme file
- create test classes for the service and controller logic

## Getting exchange rate list from Bundesbank
let's implement exchange rate retrieval from the Bundesbank

for that:
- update ExchangeRateImporter: Right after currencies are imported, exchange rates must be imported.
  Here is an API endpoint: https://api.statistiken.bundesbank.de/rest/data/BBEX3/D.USD.EUR.BB.AC.000?format=sdmx_json&detail=dataonly
  Substitute hard-coded 'USD' currency in the URL with the all currencies retrieved from the previous get all currencies API call
  Sample API call JSON response with the expected structure is attached.
- create ImportedExchangeRate java record and use it the same way as for the currency logic.