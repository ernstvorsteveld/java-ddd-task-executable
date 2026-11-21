# Tasks: Task Executability

- [x] 1. Task Executability (`ports.in` TDD)
  - [x] 1.1 Write failing `ports.in.TaskPlanningPortTest` tests for `1.1`–`1.5` and stub `Executable`, `ExecutableContext`, `DefaultExecutableContext`, `Task.isExecutable(...)`, and `ExecutableContextResolver`
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
  - [x] 1.2 Implement `Executable`, `ExecutableContext`, `DefaultExecutableContext`, `Task.isExecutable(...)`, `ExecutableContextResolver`, and `TaskPlanningService` executability filtering until all `ports.in` tests and `HexagonalArchitectureTest` rules pass
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_
- [x] 2. Multiple `ExecutableContext` Providers per `TaskType` (`ports.in` TDD)
  - [x] 2.1 Update `ports.in.TaskPlanningPortTest` (`TaskExecutabilitySpec`) to pass `Map<TaskType, List<Function<Task, ExecutableContext>>>` and add failing test coverage for multiple `ExecutableContext` providers per `TaskType` and empty provider lists
    - _Requirements: 1.2, 1.5_
  - [x] 2.2 Update `ExecutableContextResolver` to accept `Map<TaskType, List<Function<Task, ExecutableContext>>>` and combine predicates from all resolved contexts per `TaskType`, then verify with `./gradlew test`
    - _Requirements: 1.2, 1.5_
- [x] 3. Load Domain Entities and Aggregates into `ExecutableContext` (`ports.in` TDD)
  - [x] 3.1 Write failing `ports.in.TaskPlanningPortTest` (`TaskExecutabilitySpec`) tests covering loading `EntityType1`–`EntityType4` into `ExecutableContext` via `LoadedDomainObjects` across single/multiple providers, skipping loading when `TaskType` has no providers, and handling missing linked entities without failing queries
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_
  - [x] 3.2 Implement `DomainObject` (`EntityType1`–`EntityType4`), `LoadedDomainObjects`, `ExecutableContext.domainObjects()`, `DefaultExecutableContext(LoadedDomainObjects, List<Predicate<Task>>)`, outbound ports `EntityType1Repository`–`EntityType4Repository`, `ExecutableContextResolver` loading orchestration, and in-memory outbound adapters `InMemoryEntityType1Repository`–`InMemoryEntityType4Repository`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5_
  - [x] 3.3 Run `./gradlew test` and confirm all `ports.in` tests and `HexagonalArchitectureTest` rules pass
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 2.1, 2.2, 2.3, 2.4, 2.5_
