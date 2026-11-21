# Ports Layer Rules (`com.sternitc.task.ports`)

## Structure

- `com.sternitc.task.ports.in`: Driving (inbound) port interfaces, command records, query records, and inbound result types.
- `com.sternitc.task.ports.out`: Driven (outbound) SPI/repository interfaces.

## Dependency Rules

- **Allowed**: `com.sternitc.task.domain.model` types and Java 27 standard library (`java.*`) ONLY.
- **Forbidden**:
  - Never import `com.sternitc.task.application.*` or `com.sternitc.task.domain.services.*`.
  - Never import `com.sternitc.task.adapters.*`.
  - Never import third-party frameworks or annotations.
  - Keep `ports.in` and `ports.out` independent of each other.

## Design Guidelines

- Express port contracts using ubiquitous domain language, not transport or database concepts (no HTTP status codes, SQL, or JSON concerns).
- Prefer small, focused single-responsibility use-case interfaces in `ports.in` and cohesive SPI interfaces in `ports.out`.
- Model commands and queries as immutable Java `record` types that validate required inputs.
- Every query must always contain `pageNumber` (0-based, default `0`) and `pageSize` (default `20`) parameters.
