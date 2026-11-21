# Architecture & DDD Rules

This project uses **Domain-Driven Design (DDD)** with a **Hexagonal Architecture (Ports & Adapters)** in a single Gradle project under base package `com.sternitc.task`.

## Package Structure

All production code lives under `src/main/java/com/sternitc/task/` in 4 top-level packages:

- `com.sternitc.task.domain`
  - `com.sternitc.task.domain.model`: Domain core classes — Entities, Aggregate Roots, Value Objects, Domain Events, and Domain Exceptions.
  - `com.sternitc.task.domain.services`: Pure Domain Services (stateless domain logic spanning multiple aggregates/value objects; zero I/O).
- `com.sternitc.task.application`: Application services / use-case implementations that implement `ports.in` and coordinate `domain.model`, `domain.services`, and `ports.out` (orchestration only — zero business rules). Every use-case or query interface in `ports.in` must have its own separate implementation class in `application` (1-to-1; never combine multiple `ports.in` interfaces into one class).
- `com.sternitc.task.ports`
  - `com.sternitc.task.ports.in`: Driving (inbound) port interfaces, command/query records, and port-level DTOs/results. Every query must always contain `pageNumber` (0-based, default `0`) and `pageSize` (default `20`) parameters.
  - `com.sternitc.task.ports.out`: Driven (outbound) SPI/repository interfaces. Every query must always contain `pageNumber` (0-based, default `0`) and `pageSize` (default `20`) parameters.
- `com.sternitc.task.adapters`
  - `com.sternitc.task.adapters.in`: Driving adapters (e.g., CLI, HTTP, messaging consumers) that invoke `ports.in`.
  - `com.sternitc.task.adapters.out`: Driven adapters (e.g., persistence, async event bus, external clients) that implement `ports.out`.

## Dependency Rules (Cycle-Free & ArchUnit-Enforced)

1. **`domain` core** (`domain.model` and `domain.services`):
   - All domain model classes live under `com.sternitc.task.domain.model`; all domain services live under `com.sternitc.task.domain.services`.
   - `domain.model` depends **only on `domain.model` and the JDK** (zero I/O; never depends on `domain.services`, `application`, `ports`, `adapters`, or third-party libraries).
   - `domain.services` depends **only on `domain.model`, `domain.services`, and the JDK** (zero I/O; never depends on `application`, `ports`, `adapters`, or third-party libraries).
2. **`ports.in` and `ports.out`**:
   - May depend **only on `domain.model` and the JDK**.
   - Must never import `domain.services`, `application`, `adapters`, or third-party frameworks.
   - `ports.in` and `ports.out` should not depend on each other.
3. **`application`** (`com.sternitc.task.application`):
   - Implements `ports.in` and orchestrates `domain` core (`domain.model`, `domain.services`) and `ports.out` (orchestration only — zero business rules).
   - Must never import `adapters` or third-party frameworks.
4. **`adapters.in`**:
   - Calls `ports.in` (and uses `domain.model` types exposed by port contracts).
   - Must never depend on `adapters.out`, `ports.out`, or `application` concrete classes.
5. **`adapters.out`**:
   - Implements `ports.out` and translates between external representations and `domain.model` types.
   - Must never depend on `adapters.in`, `ports.in`, or `application`.

*(Composition/wiring of concrete `adapters.in`, `application`, and `adapters.out` belongs at the outermost application entry point, outside the domain core and ports.)*

## DDD Alignment

All strategic and tactical Domain-Driven Design rules (Ubiquitous Language, Aggregates, Entities, Value Objects, Domain Events, Repositories, and anti-patterns) are defined in `.agents/rules/ddd.md`.
