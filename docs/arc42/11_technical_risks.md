[« Architecture overview](README.md)

# 11. Risks and Technical Debt

- **In-memory exchange-rate cache doesn't survive a restart or scale across instances.**
  `ExchangeRateService`'s cache is a single `AtomicReference` in memory. Fine for a single
  instance (it's a once-a-day HNB call either way); a horizontally-scaled deployment would
  re-fetch once per instance per day rather than sharing a cache (not optimal)
  optimal. A shared cache (Redis, or a scheduled job populating a DB table) would be the fix if
  this ever needed to scale out.
- **No authentication or authorization.** Out of scope per the assignment ("a simple REST
  service... without frontend"), but a real deployment would need this before being reachable
  from anywhere but a trusted network.
- **Only create + view, per the assignment.** No `PUT`/`PATCH`/`DELETE`. Not an oversight, just
  not asked for. Filtering on `GET /products` (e.g. by `is_available`, or a `price_eur` range)
  is a natural next addition if the catalog grows.
- **`@Digits(integer = 9)` on `price_eur` is a workaround, not a business rule.** It exists to
  keep the derived `price_usd` from overflowing `NUMERIC(12,2)` at any realistic EUR/USD rate.
  If the business ever needed genuinely larger prices, the column width and this bound need to
  grow together, deliberately, not independently.
- **Pagination defaults (page size 20, max 100) are reasonable guesses, not derived
  requirements.** No stated requirement constrains these; they're picked to be sane defaults for
  a small catalog.
