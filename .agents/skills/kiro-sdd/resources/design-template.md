# Design Document: <Feature Name>

## Overview

<Concise summary of how this feature fits into `com.sternitc.task`.>

## Architecture & Package Mapping

### Domain Core (`com.sternitc.task.domain`)
- **Aggregates / Entities**: <Types, identities, and state transitions>
- **Value Objects**: <Java `record` types and invariants>
- **Domain Events / Exceptions**: <Events and expressive domain exceptions>

### Ports (`com.sternitc.task.ports`)
- **Inbound Ports (`ports.in`)**: <Use-case interfaces, commands, queries>
- **Outbound Ports (`ports.out`)**: <Repository / SPI interfaces>

### Application Services (`com.sternitc.task.domain.application`)
- <Use-case implementations implementing `ports.in` and orchestrating `domain` + `ports.out`>

### Adapters (`com.sternitc.task.adapters`)
- **Driving Adapters (`adapters.in`)**: <Entry points calling `ports.in`>
- **Driven Adapters (`adapters.out`)**: <Concrete implementations of `ports.out`>

## Error Handling & Invariants

| Requirement | Condition | Domain Exception / Result |
| :--- | :--- | :--- |
| 1.2 | <Invalid input or state> | <Exception / sealed result> |

## Testing Strategy

- **Inbound Port Tests (`ports.in`) Only**: JUnit 5 `@Nested` / `@DisplayName` + AssertJ tests invoking `ports.in` exclusively and covering every EARS criterion (`1.1`, `1.2`, ...). No isolated unit tests and no code mocking.
- **Real Wiring & External Systems**: Wire real `domain.application`, `domain` core, and `adapters.out`.
  - Standard infrastructure (databases, Kafka, etc.): **Testcontainers** (list containers needed, or `None`).
  - External HTTP APIs: **WireMock** (list stubbed endpoints, or `None`).
- **Architecture Tests**: ArchUnit rules confirming cycle-free hexagonal dependencies.
