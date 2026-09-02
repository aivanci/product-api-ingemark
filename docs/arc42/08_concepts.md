[« Architecture overview](README.md)

# 8. Cross-cutting Concepts

## 8.1 Wire format & JSON naming

The assignment's field names are snake_case (`price_eur`, `price_usd`, `is_available`); Java
code stays idiomatic camelCase. Every DTO field is explicitly annotated with `@JsonProperty`
rather than relying on a global Jackson naming strategy. The global snake_case strategy doesn't
map an `isAvailable()` boolean accessor to `is_available` cleanly, so explicit annotations are
both more correct and more obviously intentional to a reader.

## 8.2 Validation & error handling

Bean Validation (`@Valid` + Jakarta constraints) on inbound DTOs; a single
`@RestControllerAdvice` (`GlobalExceptionHandler`) is the only place that maps exceptions to
HTTP status, and all error responses share one `ApiError` shape. Validation `fieldErrors` are
keyed by the wire name the client actually sent (`price_eur`), not the Java property name
(`priceEur`), so a small translation map exists specifically so the contract stays consistent for
the client, rather than incidentally leaking an implementation detail.

## 8.3 Persistence & schema ownership

Flyway migrations are the single source of truth for the schema; Hibernate's `ddl-auto` is
`validate`, never `update`. The entity mapping is checked against the real schema at startup
rather than allowed to silently diverge from it or auto-generate DDL. Money is `BigDecimal` in
Java and `NUMERIC(12,2)` in Postgres throughout.

## 8.4 Testing strategy

Each layer is tested at the level that actually exercises it:

- **Unit tests** (Mockito) for business logic in isolation: `ProductServiceTest`,
  `ExchangeRateServiceTest`.
- **A web slice test** (`@WebMvcTest`) for the HTTP layer: `ProductControllerTest`.
- **A focused Spring-context test** (`HnbApiClientTest`) for the one thing that genuinely
  requires a real `ApplicationContext` to verify: `@Retryable`'s AOP behavior, which a plain
  unit test cannot exercise, since the retry logic lives in a runtime-generated proxy rather
  than code that can be called directly.
- **One full-stack integration test** (`ProductApiIntegrationTest`, Testcontainers Postgres)
  exercising real HTTP, real schema constraints, and the real Flyway migration. The HNB API is
  the one boundary deliberately stubbed there, as it is a third-party service whose own parsing/caching
  logic is already covered by `ExchangeRateServiceTest` and `HnbApiClientTest`, and re-verifying
  it against the live API on every test run would make the suite flaky and slow for no added
  confidence.

## 8.5 Lombok usage convention

Scoped deliberately, not applied uniformly. `@Getter` plus a protected no-args constructor on
the one JPA entity. Never `@Data`/`@Setter`/`@EqualsAndHashCode`/`@ToString` on it (setters
would undermine construct-once-never-mutate; field-based `equals`/`hashCode` is unsafe with
Hibernate proxies and lazy state). `@RequiredArgsConstructor` for plain constructor injection
wherever every dependency is a bean; a hand-written constructor where a class mixes a bean
dependency with `@Value`-injected primitives.