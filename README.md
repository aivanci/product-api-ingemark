# Product API

Spring REST API assignment (Assignment #1) for Ingemark. A REST service that manages a catalog of
products, with `price_usd` calculated server-side from `price_eur` using the Croatian National Bank
(HNB) daily exchange rate.

## Tech stack

- Java 17
- Spring Boot 3.4 (Spring Web / MVC, Spring Data JPA, Bean Validation)
- Maven (wrapper included)
- PostgreSQL 16
- Flyway (schema migrations)
- Spring Retry
- Lombok, MapStruct
- springdoc-openapi (Swagger UI)
- JUnit 5, Mockito, AssertJ, Spring `MockMvc` / `MockRestServiceServer`, Testcontainers

## Prerequisites

- JDK 17+
- Docker (for the local Postgres instance) — or a Postgres 16 instance of your own
