[« Architecture overview](README.md)

# 9. Architecture Decisions

## ADR-1: `price_usd` is always computed server-side

**Context:** The assignment requires `price_usd` to be calculated via the HNB API.
**Decision:** `ProductRequest` has no `priceUsd` field at all: it cannot be supplied, only
computed.
**Consequences:** A client cannot spoof the derived price; the rule is enforced by the request
DTO's shape itself, not a runtime check that could be bypassed or forgotten.

## ADR-2: `createProduct` is not `@Transactional`

**Context:** Creating a product involves a DB write and, on a cache miss, an outbound HTTP call
to HNB.
**Decision:** No transaction wraps the whole method; only the individual repository calls are
transactional.
**Consequences:** A slow or failing external call never holds a pooled DB connection open.
Trade-off: the uniqueness check and the insert aren't atomic with each other, and this is accepted, because
the DB unique constraint (ADR-5) is the actual correctness guarantee, not the pre-check.

## ADR-3: `HnbApiClient` is a separate bean from `ExchangeRateService`

**Context:** `@Retryable` needs to actually fire on HNB call failures.
**Decision:** The HTTP call was extracted from `ExchangeRateService` into its own bean,
`HnbApiClient`, called across a real bean boundary.
**Consequences:** Spring AOP proxies only advise calls arriving from outside a bean; a
self-invoked call (one method of a class calling another method of the same class) bypasses the
proxy entirely, and `@Retryable` would silently never fire. 

## ADR-4: MapStruct for `ProductMapper`

**Context:** `Product` → `ProductResponse` is a 1:1 field mapping.
**Decision:** Use MapStruct (compile-time code generation) rather than the handful of lines of
hand-written mapping it replaces.
**Consequences:** Arguably more machinery than this specific mapping needs on its own; included
to demonstrate the pattern, including its interaction with Lombok (both are annotation
processors, and getting their ordering right via `lombok-mapstruct-binding` is itself worth
showing). A field renamed on one side but not the other now fails at compile time instead of
silently at runtime.

## ADR-5: Defense-in-depth for code uniqueness

**Context:** `code` must be unique; a naive check-then-insert has a race window between two
concurrent requests.
**Decision:** Enforce uniqueness at three levels: a Postgres `UNIQUE` constraint (the actual
guarantee), an `existsByCode` pre-check (fast, clean `409` for the common non-concurrent case),
and a catch around `save()` that maps a DB-level unique violation to the same `409` (covers the
race).
**Consequences:** The common case gets a clear, cheap `409` without a failed insert attempt; the
race case still resolves correctly, just via the DB constraint instead of the pre-check.

## ADR-6: Custom `PageResponse` envelope instead of exposing `Page` directly

**Context:** `GET /products` needed pagination.
**Decision:** Return a small stable record (`PageResponse<T>`) rather than serializing Spring
Data's `Page`/`PageImpl` directly.
**Consequences:** `Page`'s JSON shape is an implementation detail not guaranteed across Spring
versions (Spring Boot itself warns against returning it from a controller); the custom envelope
is a contract this project actually controls.

## ADR-7: Sort property allowlist

**Context:** `GET /products?sort=...` needed to accept client-specified sort fields.
**Decision:** Wire names are explicitly mapped to entity properties (`price_eur` → `priceEur`)
via an allowlist in `ProductService`, rather than passing the client's `sort` parameter straight
to Spring Data.
**Consequences:** The sort contract matches the JSON response field names, not Java property
names; an unrecognized property returns a clean `400` instead of a `PropertyReferenceException`
surfacing as an unhandled `500`.
