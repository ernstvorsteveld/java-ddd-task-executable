# Tasks: Command Processing

- [x] 1. Gradle Dependencies
  - [x] 1.1 Add `mongodb-driver-sync`, `testcontainers-bom:2.0.5` (`testcontainers-junit-jupiter`, `testcontainers-mongodb`), and `awaitility` to `gradle/libs.versions.toml`, `build.gradle.kts`, and `.agents/rules/java-gradle.md`
    - _Requirements: 1.1, 2.1_
- [x] 2. Command Store, Event & Async Handling (`ports.in` TDD with Testcontainers MongoDB)
  - [x] 2.1 Write failing `ports.in.TaskPlanningPortTest` against Testcontainers MongoDB (`mongo:8.0`) + Awaitility for `1.1`–`1.2`, `2.1`–`2.2` (and all `task-planning` rules) with `domain.model`, `ports`, and adapter stubs
    - _Requirements: 1.1, 1.2, 2.1, 2.2_
  - [x] 2.2 Implement `StoredCommand`, `TaskCommandPayload`, `CommandStoredEvent`, `TaskPlanningService`, `MongoCommandRepository`, `MongoTaskRepository`, `VirtualThreadCommandEventBus`, and `UuidCommandIdGenerator` (removing `InMemoryTaskRepository`) until all `ports.in` tests pass
    - _Requirements: 1.1, 1.2, 2.1, 2.2_
- [x] 3. REST Adapter (`202 Accepted` & `500` Uniform Error)
  - [x] 3.1 Regenerate OpenAPI stubs (`CommandAcceptedResponsePayload`) and update `TaskRestController` (`POST`/`PUT` -> `202 Accepted`) and `TaskRestExceptionHandler` (`500` handler)
    - _Requirements: 1.1, 1.2_
- [x] 4. Full Verification
  - [x] 4.1 Run `./gradlew openApiGenerate test` and confirm all Testcontainers MongoDB `ports.in` tests and `HexagonalArchitectureTest` rules pass
    - _Requirements: 1.1, 1.2, 2.1, 2.2_
