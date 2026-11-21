# Domain-Driven Design (DDD) Rules

All domain modeling under `com.sternitc.task.domain` and port design under `com.sternitc.task.ports` must follow these strategic and tactical DDD principles.

## Strategic Design

- **Ubiquitous Language**: Use the exact domain terminology agreed upon in `.kiro/specs/` consistently across packages, types, methods, commands, queries, events, and tests. Avoid generic technical names (`Manager`, `Helper`, `Processor`, `ServiceImpl`, `Data`, `DTO`) inside `domain` and `ports`.
- **Bounded Context Integrity & Anti-Corruption Layer (ACL)**: Keep external APIs, messaging schemas, and persistence models strictly inside `adapters`. Always translate between external representations and domain types at the adapter boundary so foreign concepts never leak into `domain` or `ports`.

## Tactical Building Blocks (`domain` Core)

- **Aggregates & Aggregate Roots**
  - An Aggregate is a **transactional consistency boundary**; keep aggregates small and cohesive, grouping only what must remain immediately consistent together.
  - External callers (`application`, `ports.out` repositories, adapters) must reference and mutate an aggregate **exclusively through its Aggregate Root**.
  - Reference other aggregates **by typed Identity Value Object only** (e.g., `ProjectId`), never by direct object reference to another Aggregate Root.
  - Prefer modifying **one aggregate per use-case transaction**; coordinate cross-aggregate side effects through Domain Events.

- **Entities**
  - Defined by a **typed Identity** (`record` Value Object), not by mutable attributes; implement `equals` and `hashCode` based on identity.
  - Enforce invariants on every state transition through intention-revealing domain methods (`complete(...)`, `assignTo(...)`); never expose public setters or mutable internal collections.

- **Value Objects**
  - Model as immutable, self-validating Java `record`s with structural equality.
  - Enforce invariants eagerly in compact constructors so invalid Value Objects cannot exist.
  - Prefer Value Objects over Entities whenever continuity of identity is not required, and place cohesive domain calculations/rules directly on them.

- **Domain Services**
  - Use only for stateless domain rules or policies that naturally span multiple aggregates or value objects and do not belong on a single entity or value object.
  - Must stay pure `domain` core (zero I/O, zero calls to `ports.out`).

- **Domain Events**
  - Model as immutable Java `record`s named in the **past tense** (e.g., `TaskCreated`, `TaskCompleted`) carrying typed Value Objects and an explicit occurrence `Instant`.
  - Represent meaningful business facts that occurred within the domain.

- **Factories & Reconstitution**
  - Use expressive static creation methods (`create(...)`) when creation enforces domain rules or raises initial Domain Events.
  - Provide a dedicated reconstitution path (`reconstitute(...)`) for restoring persisted state without re-emitting creation events.

- **Specifications & Policies**
  - Express complex domain eligibility, validation, or transition rules as explicit policy or specification types in `domain` core rather than scattering nested conditionals across callers.

## Repositories & Application Layer Boundary

- **Repository per Aggregate Root (`ports.out`)**: Define repository interfaces only for Aggregate Roots, using domain collection terminology (`save`, `findById`, etc.). Never expose SQL, ORM, or transport abstractions in `ports.out`.
- **Thin Application Services (`application`)**: Create a separate implementation class for each use-case or query interface in `ports.in` (1-to-1). Orchestrate a single use case or query (load aggregate(s) via `ports.out` -> invoke domain behavior -> persist via `ports.out` -> publish/dispatch domain events). Keep business rules and invariant checks out of `application`.

## DDD Anti-Patterns to Reject

- **Anemic Domain Model**: Data-only entities with getters/setters while business rules live in `application` or adapters.
- **God Aggregates**: Overly large aggregates or unbounded child collections loaded as a single consistency boundary.
- **Cross-Aggregate Object References**: Holding direct references to another Aggregate Root instead of its typed ID.
- **Bypassing the Aggregate Root**: Directly mutating or persisting child entities outside their root.
- **Framework / Persistence Leakage**: Placing database, JSON, HTTP, or DI annotations or types in `domain` or `ports`.
