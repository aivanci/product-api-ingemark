# Product API

Spring REST API assignment (Assignment #1) for Ingemark — a REST service that manages a catalog of
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

No local Maven installation is needed as the project ships with the Maven wrapper
(`./mvnw` on Linux/macOS/Git Bash, `mvnw.cmd` on Windows cmd/PowerShell), which downloads
Maven 3.9 on first use.

## Local setup

1. **Clone the repository**

   ```bash
   git clone <repo-url>
   cd product-api
   ```

2. **Start Postgres**

   ```bash
   docker compose up -d
   ```

This starts Postgres 16 on `localhost:5432` with database `productdb`, user `postgres`,
password `postgres`. Flyway creates the schema automatically on application startup.

3. **Run the application**

   ```bash
   ./mvnw spring-boot:run
   ```

   The API is available at `http://localhost:8080`. Interactive API documentation (Swagger UI)
   is served at `http://localhost:8080/swagger-ui.html`, and the raw OpenAPI spec at
   `http://localhost:8080/v3/api-docs`.

4. **Run the tests**

   ```bash
   ./mvnw test
   ```

Unit  tests (Mockito for the service layer, `MockRestServiceServer` for the HNB
integration, `@WebMvcTest` for the controller) are self-contained and need neither Postgres
nor Docker. `ProductApiIntegrationTest` additionally boots the full application against a
throwaway Postgres started via Testcontainers. It requires Docker and is skipped 
automatically when Docker is not available.

### Configuration

All configuration has sane local defaults (see `application.yml`) and can be overridden via
environment variables:

| Variable                | Default                                        | Purpose                          |
|--------------------------|------------------------------------------------|-----------------------------------|
| `DB_URL`                 | `jdbc:postgresql://localhost:5432/productdb`   | JDBC connection string            |
| `DB_USERNAME`             | `postgres`                                     | DB username                       |
| `DB_PASSWORD`             | `postgres`                                     | DB password                       |
| `HNB_BASE_URL`            | `https://api.hnb.hr/tecajn-eur/v3`             | HNB exchange rate list endpoint   |
| `HNB_CONNECT_TIMEOUT_MS`  | `3000`                                         | HNB HTTP connect timeout          |
| `HNB_READ_TIMEOUT_MS`     | `5000`                                         | HNB HTTP read timeout             |
| `HNB_RETRY_MAX_ATTEMPTS`  | `3`                                             | Max attempts per HNB call (incl. the first) |
| `HNB_RETRY_DELAY_MS`      | `500`                                          | Fixed delay between retry attempts |
| `SERVER_PORT`             | `8080`                                         | Application port                  |

## API reference

Base path: `/api/v1/products`

### `POST /api/v1/products` — create a product

`price_usd` is **not** part of the request and it's always computed server-side.

```bash
curl -i -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{
        "code": "ABCDEFGHIJ",
        "name": "Widget",
        "price_eur": 100.00,
        "is_available": true
      }'
```

Response `201 Created` (with `Location: /api/v1/products/1`):

```json
{
  "id": 1,
  "code": "ABCDEFGHIJ",
  "name": "Widget",
  "price_eur": 100.00,
  "price_usd": 116.45,
  "is_available": true
}
```

### `GET /api/v1/products/{id}` — view a specific product

`200 OK` with the same shape as above, or `404 Not Found` if the id doesn't exist.

### `GET /api/v1/products` — list products (paginated, sortable)

`200 OK` with a pagination envelope. Query parameters:

| Parameter | Default | Notes                                                                 |
|-----------|---------|------------------------------------------------------------------------|
| `page`    | `0`     | Zero-indexed                                                            |
| `size`    | `20`    | Max `100`                                                                |
| `sort`    | `id,asc`| Repeatable, e.g. `?sort=price_eur,desc&sort=name,asc`. Sortable properties: `id`, `code`, `name`, `price_eur`, `price_usd`, `is_available` (wire names — the same fields you see in the response). An unrecognized property returns `400 Bad Request` rather than an internal error. |

```bash
curl "http://localhost:8080/api/v1/products?page=0&size=10&sort=price_eur,desc"
```

```json
{
  "content": [
    { "id": 1, "code": "ABCDEFGHIJ", "name": "Widget", "price_eur": 100.00, "price_usd": 116.45, "is_available": true }
  ],
  "page": 0,
  "size": 10,
  "total_elements": 1,
  "total_pages": 1
}
```

### Validation & error responses

All error responses share one shape:

```json
{
  "timestamp": "2026-08-28T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/products",
  "fieldErrors": { "code": "code must be exactly 10 characters" }
}
```

| Condition                                   | Status                     |
|----------------------------------------------|----------------------------|
| Invalid/missing request fields                | `400 Bad Request`          |
| Unknown `sort` property on `GET /products`    | `400 Bad Request`          |
| Product id not found (`GET /{id}`)            | `404 Not Found`            |
| `code` already exists                         | `409 Conflict`             |
| HNB exchange rate service unreachable/invalid (after retries) | `503 Service Unavailable`  |
| Anything unexpected                           | `500 Internal Server Error`|

