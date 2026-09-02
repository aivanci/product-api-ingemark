[« Architecture overview](README.md)

# 6. Runtime View

## 6.1 Creating a product (`POST /products`)

The only flow with genuine complexity — everything else is a straightforward read.

```mermaid
sequenceDiagram
    participant C as Client
    participant Ctrl as ProductController
    participant Svc as ProductService
    participant Repo as ProductRepository
    participant Rate as ExchangeRateService
    participant Hnb as HnbApiClient
    participant DB as PostgreSQL
    participant HNB as HNB API

    C->>Ctrl: POST /products
    Ctrl->>Svc: createProduct(request)
    Svc->>Repo: existsByCode(code)
    Repo->>DB: SELECT
    DB-->>Repo: false
    Svc->>Rate: convertEurToUsd(priceEur)
    alt rate cached for today
        Rate-->>Svc: cached rate
    else cache miss
        Rate->>Hnb: fetchExchangeRateList()
        Hnb->>HNB: GET tecajn-eur/v3
        HNB-->>Hnb: rate list (retried on transient failure)
        Hnb-->>Rate: rates
        Rate-->>Svc: computed rate
    end
    Svc->>Repo: save(product)
    Repo->>DB: INSERT
    DB-->>Repo: OK (or unique violation)
    Repo-->>Svc: saved product
    Svc-->>Ctrl: ProductResponse
    Ctrl-->>C: 201 Created
```

Two things the diagram doesn't show: the `existsByCode` check and the eventual `save()` aren't
atomic with each other — a genuine race between two concurrent requests with the same `code` is
still caught, because a DB-level unique violation on `save()` maps to the same `409` (see
[Architecture Decisions, ADR-5](09_architecture_decisions.md)). And the whole method
deliberately isn't wrapped in `@Transactional` (see
[Architecture Decisions, ADR-2](09_architecture_decisions.md)).
