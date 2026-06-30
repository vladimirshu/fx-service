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