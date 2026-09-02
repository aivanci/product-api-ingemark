[« Architecture overview](README.md)

# 1. Introduction and Goals

## 1.1 Requirements Overview

A REST service managing a catalog of products (Ingemark take-home Assignment #1): create a
product, view one by id, view the list. `price_usd` is not client-supplied — it's calculated
server-side from `price_eur` via the Croatian National Bank's (HNB) daily exchange rate.
Product fields: `id`, `code` (unique, exactly 10 characters), `name`, `price_eur` (≥ 0),
`price_usd` (≥ 0, computed), `is_available`. Required stack: Java 17+, Spring Boot + MVC,
Postgres, a persistence technology of choice, delivered with a README covering local setup.

## 1.2 Quality Goals

| Priority | Goal | What that means concretely here |
|---|---|---|
| 1 | Correctness | `price_usd` always reflects the current-day HNB middle rate; `code` uniqueness holds even under concurrent requests |
| 2 | Resilience | A transient HNB outage doesn't fail every product creation outright — retried, then a clear `503` rather than a stack trace |
| 3 | Testability | Each layer (business logic, web layer, the one AOP-dependent behavior, the full stack) is verifiable in isolation, without needing the whole stack running |
| 4 | Explicitness over convenience | The wire contract, sort keys, and validation errors match exactly what the client sent — no internal Java naming leaking through incidentally |

## 1.3 Constraints

- **Given by the assignment:** Java 17+, Maven or Gradle, Git, Spring Boot + Spring MVC,
  Postgres, a persistence technology of choice, no frontend, delivered as a repository link.
- **Self-imposed:** none beyond the above — deliberately no framework or infrastructure not
  needed to satisfy the stated requirements (see [§4 Solution Strategy](04_solution_strategy.md)).
