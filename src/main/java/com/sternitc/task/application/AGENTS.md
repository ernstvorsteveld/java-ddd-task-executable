# Application Layer Rules (`com.sternitc.task.application`)

## Responsibility

- Contains application services / use-case implementations implementing `com.sternitc.task.ports.in.*` interfaces.
- **One implementation per inbound port (1-to-1)**: create a separate implementation class for every use-case or query interface in `ports.in` (never combine multiple `ports.in` interfaces into one class).
- Orchestrates `com.sternitc.task.domain.model`, `com.sternitc.task.domain.services`, and `com.sternitc.task.ports.out.*`.
- **Orchestration only — zero business rules**: push all domain invariants and business rules down into `domain.model` and `domain.services`.

## Dependency Rules

- **Allowed**: `com.sternitc.task.domain.*`, `com.sternitc.task.ports.in.*`, `com.sternitc.task.ports.out.*`, and Java 27 standard library (`java.*`) ONLY.
- **Forbidden**: imports from `com.sternitc.task.adapters.*` or third-party frameworks.
