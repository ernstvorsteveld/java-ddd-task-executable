# Domain Layer Rules (`com.sternitc.task.domain`)

## Package Layout

- `com.sternitc.task.domain.model`: Entities, Aggregate Roots, Value Objects, Domain Events, and Domain Exceptions.
- `com.sternitc.task.domain.services`: Pure Domain Services (stateless domain logic spanning multiple aggregates/value objects; zero I/O).

## Dependency Rules

- **Dependencies**: Java 27 standard library (`java.*`) and `domain.model` ONLY (`domain.model` must not depend on `domain.services`).
- **Forbidden**: imports from `com.sternitc.task.application`, `com.sternitc.task.ports`, `com.sternitc.task.adapters`, or any third-party/framework library.
- Zero I/O inside `domain.model` and `domain.services`.
- Enforce all domain invariants in constructors, compact constructors, and domain methods.
- Prefer immutable `record` and `sealed` types; never expose mutable internal collections.
