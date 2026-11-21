# Design: Task Executability

## Domain Core (`com.sternitc.task.domain.model`)
- `interface Executable` — `boolean isExecutable(ExecutableContext context)`.
- `sealed interface DomainObject permits EntityType1, EntityType2, EntityType3, EntityType4` — exposes `EntityTypeName typeName()`, `EntityId entityId()`, and `default LinkedEntity reference()`.
  - `record EntityType1(EntityId entityId) implements DomainObject` (`ENTITY_TYPE_1`)
  - `record EntityType2(EntityId entityId) implements DomainObject` (`ENTITY_TYPE_2`)
  - `record EntityType3(EntityId entityId) implements DomainObject` (`ENTITY_TYPE_3`)
  - `record EntityType4(EntityId entityId) implements DomainObject` (`ENTITY_TYPE_4`)
- `record LoadedDomainObjects(Map<LinkedEntity, DomainObject> objects)` — immutable container of loaded domain entities/aggregates keyed by `LinkedEntity`, providing `empty()`, `of(Collection<? extends DomainObject>)`, `find(LinkedEntity)`, `find(EntityTypeName, EntityId)`, `findByType(EntityTypeName)`, `findByType(EntityTypeName, Class<T>)`, `contains(LinkedEntity)`, and `isEmpty()`.
- `interface ExecutableContext` — `List<Predicate<Task>> predicates()`, `default LoadedDomainObjects domainObjects()` (defaulting to `LoadedDomainObjects.empty()`), and `default boolean evaluate(Task task)` returning `true` when all predicates match `task`.
- `class DefaultExecutableContext implements ExecutableContext`:
  - `DefaultExecutableContext()` — empty `LoadedDomainObjects` and empty predicate list (returns `true`).
  - `DefaultExecutableContext(List<Predicate<Task>> predicates)` — empty `LoadedDomainObjects` and defensively copied non-null predicates (`List.copyOf`).
  - `DefaultExecutableContext(LoadedDomainObjects domainObjects, List<Predicate<Task>> predicates)` — defensively holds non-null `domainObjects` and non-null predicates (`List.copyOf`).
- `Task implements Executable` — `isExecutable(ExecutableContext context)` validates non-null `context` and returns `context.evaluate(this)`.
- Any future per-`TaskType` `ExecutableContext` implementation lives in `com.sternitc.task.domain.model` (pure JDK), accepting `LoadedDomainObjects` / typed domain entities/aggregates and its `List<Predicate<Task>>` rules.

## Outbound Ports (`com.sternitc.task.ports.out`)
- One outbound port per linked entity type:
  - `EntityType1Repository`: `Optional<EntityType1> findById(EntityId entityId)`
  - `EntityType2Repository`: `Optional<EntityType2> findById(EntityId entityId)`
  - `EntityType3Repository`: `Optional<EntityType3> findById(EntityId entityId)`
  - `EntityType4Repository`: `Optional<EntityType4> findById(EntityId entityId)`

## Application Orchestration (`com.sternitc.task.application`)
- `ExecutableContextResolver` — holds `Map<TaskType, List<BiFunction<Task, LoadedDomainObjects, ExecutableContext>>> providersByType` and the four outbound ports (`EntityType1Repository`–`EntityType4Repository`), defensively copying map and provider lists and rejecting null keys/lists/elements.
  - `defaults()` returns an empty resolver (never loads linked entities).
  - `resolve(Task)`:
    - If no providers (or an empty list) are configured for `task.type()`, skips all outbound port calls and returns `DefaultExecutableContext`.
    - Otherwise loads each `LinkedEntity` on `task.linkedEntities()` via an exhaustive `switch` over `EntityTypeName` dispatching to the matching outbound port's `findById(entityId)`, assembling found entities into `LoadedDomainObjects` (missing entities are omitted without failing the query).
    - Invokes all configured providers with `(task, loadedDomainObjects)` and returns the single context (1 provider) or `new DefaultExecutableContext(loadedDomainObjects, combinedPredicates)` (multiple providers).
- `TaskQueryService` — evaluates `task.isExecutable(executableContextResolver.resolve(task))` for all tasks retrieved from `TaskRepository` in `findByExecutor` and `search`, returning only executable tasks.

## Outbound Adapters (`com.sternitc.task.adapters.out.inmemory`)
- Initial in-memory adapters implementing `EntityType1Repository`–`EntityType4Repository` (with `save(...)` and `findById(EntityId)`):
  - `InMemoryEntityType1Repository`
  - `InMemoryEntityType2Repository`
  - `InMemoryEntityType3Repository`
  - `InMemoryEntityType4Repository`

## Testing
- `ports.in.TaskPlanningPortTest` (`@Nested TaskExecutabilitySpec`) — queries real `Task` instances through `PlanTaskUseCase` / `TaskQuery` (`ports.in` + Testcontainers MongoDB + `InMemoryEntityType1..4Repository`) and verifies `1.1`–`1.5` and `2.1`–`2.5`: `Executable` contract, default `DefaultExecutableContext` returning `true`, `List<Predicate<Task>>` evaluation, configuring multiple `ExecutableContext` providers per `TaskType`, loading `EntityType1`–`EntityType4` into `ExecutableContext` via `LoadedDomainObjects`, skipping loading when a `TaskType` has no providers, and treating missing linked entities as absent (filtering the task out without failing the query).
- `architecture.HexagonalArchitectureTest` — verifies `domain.model` stays pure JDK, `ports.out` depends only on `domain.model` and JDK, `application` obeys all hexagonal boundaries, and `adapters.out.inmemory` stays isolated from `ports.in`, `adapters.in`, and `application`.
