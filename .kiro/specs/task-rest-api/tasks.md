# Tasks: Task REST API

- [x] 1. OpenAPI Contract & Stub Generation
  - [x] 1.1 Define `POST /tasks`, `PUT /tasks/{taskId}`, `GET /tasks`, and payloads in `src/api/task-api.yaml` and generate `TasksApi` and DTOs in `build/generated/openapi/src/main/java`
    - _Requirements: 1.1, 1.2, 1.4_
- [x] 2. REST Inbound Adapter (`adapters.in.rest`)
  - [x] 2.1 Implement `TaskRestController` (implementing `TasksApi`) delegating to `PlanTaskUseCase`, `UpdateTaskUseCase`, and `TaskQuery`
    - _Requirements: 1.1, 1.2_
  - [x] 2.2 Implement `TaskRestExceptionHandler` returning uniform `ErrorResponsePayload` for `400` and `404` responses
    - _Requirements: 1.3, 1.4_
- [x] 3. Verify Build & Architecture
  - [x] 3.1 Run `./gradlew openApiGenerate test` and confirm generation, compilation, `ports.in` tests, and `HexagonalArchitectureTest` rules pass
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
- [x] 4. Async `202 Accepted` & `500` Error Contract
  - [x] 4.1 Regenerate OpenAPI stubs (`CommandAcceptedResponsePayload`), update `TaskRestController` (`POST`/`PUT` -> `202 Accepted`) and `TaskRestExceptionHandler` (`500` handler), and verify with `./gradlew openApiGenerate test`
    - _Requirements: 1.1, 1.2, 1.3, 1.4_
