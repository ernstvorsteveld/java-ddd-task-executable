# Requirements: Task Executability

## Requirement 1: Task Executability
**User Story:** As a user, when tasks are queried, I want a task to know if it is executable so that it is presented to the user only when it is executable.

1.1. The `Task` shall implement an `Executable` interface providing `isExecutable(ExecutableContext)`.
1.2. The `ExecutableContext` shall be an interface that can be implemented one or more times per `TaskType` when needed, accepting a list of `Predicate`s (boolean expressions over task attributes or functions) in its constructor.
1.3. WHEN evaluated against a `Task`, `isExecutable(ExecutableContext)` shall return `true` if all predicates in the `ExecutableContext` evaluate to `true`, and `false` otherwise.
1.4. The system shall provide a default implementation that returns `true`.
1.5. WHEN tasks are queried (`findByExecutor` or `search`), the system shall resolve the list of `ExecutableContext`s configured for each retrieved task's `TaskType` (defaulting to the default `ExecutableContext` when none are configured), evaluate `task.isExecutable(...)` across all predicates from the resolved contexts, and return only tasks for which all predicates evaluate to `true`.

## Requirement 2: Load Domain Entities and Aggregates into `ExecutableContext`
**User Story:** As a user, I want the domain to load the linked entities and/or aggregates required for a task's `ExecutableContext` before evaluating its predicates, so that executability rules can inspect both the `Task` and its related domain state without coupling the domain model to I/O.

2.1. The domain model shall define pure domain representations for loaded linked entities and/or aggregates identified by `EntityTypeName` and `EntityId`, with zero I/O or external framework dependencies.
2.2. The `ExecutableContext` shall be able to hold loaded domain entities and/or aggregates so that its `Predicate`s can evaluate executability against both the `Task` and the loaded domain state.
2.3. WHEN resolving `ExecutableContext`s for a `Task`, the system shall load the domain entities and/or aggregates referenced by `Task.linkedEntities()` through outbound port(s) (`ports.out`) and supply them to the `ExecutableContext`(s) configured for the task's `TaskType`.
2.4. IF a `Task` has no `ExecutableContext` providers configured for its `TaskType`, THEN the system shall skip loading linked entities/aggregates and evaluate executability using the default `ExecutableContext`.
2.5. IF a linked entity or aggregate referenced by a `Task` is not found when loading, THEN the system shall still allow `ExecutableContext` evaluation without failing the query, so predicates requiring that entity or aggregate evaluate the task as not executable (`false`).
