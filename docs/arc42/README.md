# Architecture Documentation

Structured after [arc42](https://arc42.org).

## Sections

| § | Section | |
|---|---|---|
| 1 | Introduction and Goals | [01_introduction_and_goals.md](01_introduction_and_goals.md) |
| 3 | Context and Scope | [03_context_and_scope.md](03_context_and_scope.md) |
| 4 | Solution Strategy | [04_solution_strategy.md](04_solution_strategy.md) |
| 5 | Building Block View | [05_building_block_view.md](05_building_block_view.md) |
| 6 | Runtime View | [06_runtime_view.md](06_runtime_view.md) |
| 8 | Cross-cutting Concepts | [08_concepts.md](08_concepts.md) |
| 9 | Architecture Decisions | [09_architecture_decisions.md](09_architecture_decisions.md) |
| 11 | Risks and Technical Debt | [11_technical_risks.md](11_technical_risks.md) |

Numbering follows arc42's own section numbers, gaps included, so it stays cross-referenceable
against the template.

## What's not here, and why

- **§2 Constraints** — folded into §1.3 of [01_introduction_and_goals.md](01_introduction_and_goals.md)
  rather than given its own file; there are exactly four constraint bullets, all given directly
  by the assignment.
- **§7 Deployment View** — fully covered by the main [README](../../README.md)'s setup steps. A
  docker-compose Postgres and one jar don't need a second description.
- **§10 Quality Requirements** as a full scenario tree — the goals table in §1.2 of
  [01_introduction_and_goals.md](01_introduction_and_goals.md) covers it at the depth this
  project warrants.
- **§12 Glossary** — a handful of self-explanatory domain terms don't need one.
