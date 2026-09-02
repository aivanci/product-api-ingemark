[« Architecture overview](README.md)

# 4. Solution Strategy

- Conventional layered architecture: controller → service → repository/mapper/client —
  proportionate to a 3-endpoint service
- Spring Data JPA + Flyway for persistence, with Flyway as the single source of truth for the
  schema (see [Cross-cutting Concepts, §8.3](08_concepts.md)).
- The one external dependency (HNB) is isolated behind a single-purpose client bean
  (`HnbApiClient`), keeping retry and resilience concerns out of business logic entirely
  (see [Architecture Decisions, ADR-3](09_architecture_decisions.md)).
- Money handled as `BigDecimal` end-to-end, matching the database's `NUMERIC` columns 
- The wire contract (JSON field names, error shapes, sort keys) is treated as an explicit,
  tested contract, not an incidental reflection of Java naming (see
  [Cross-cutting Concepts, §8.1](08_concepts.md) and
  [Architecture Decisions, ADR-7](09_architecture_decisions.md)).
