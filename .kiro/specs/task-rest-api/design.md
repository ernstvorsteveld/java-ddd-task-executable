# Design: Task REST API

## OpenAPI Contract (`src/api/task-api.yaml`)
- Endpoints: `POST /tasks` (`createTask` -> `202`), `PUT /tasks/{taskId}` (`updateTask` -> `202`), `GET /tasks` (`searchTasks` -> `200`).
- Payloads: `CreateTaskRequestPayload` (including optional `linkedEntities` array of `LinkedEntityDto(typeName: EntityTypeNameDto, entityId)`), `UpdateTaskRequestPayload`, `CommandAcceptedResponsePayload` (`commandId`, `timestamp`), `TaskResponsePayload` (including required `linkedEntities`), `TasksResponsePayload` (`tasks`, `count`, `pageNumber`, `pageSize`, and optional filter query parameters), `ErrorResponsePayload` (`errors`: list of `ErrorItem(errorCode, timestamp, errorDescription)`).
- Server stubs (`TasksApi` and DTOs) generated into `build/generated/openapi/src/main/java` under `com.sternitc.task.api` (API interfaces) and `com.sternitc.task.api.model` (DTOs).

## Inbound REST Adapter (`com.sternitc.task.adapters.in.rest`)
- `TaskRestController implements TasksApi` — Anti-Corruption Layer translating REST payloads <-> `domain.model` / `ports.in`; delegates strictly to `PlanTaskUseCase`, `UpdateTaskUseCase`, and `TaskQuery` (`202 Accepted` with `CommandAcceptedResponsePayload` on `POST`/`PUT`, `200 OK` with `TasksResponsePayload` on `GET`).
- `TaskRestExceptionHandler` (`@RestControllerAdvice`) — maps domain/request validation errors to `400 Bad Request`, `TaskNotFoundException` to `404 Not Found`, and unexpected errors to `500 Internal Server Error` using uniform `ErrorResponsePayload`.

## Verification
- `adapters.in.rest.TaskRestControllerTest` — boots embedded Spring Boot Tomcat on a random port wired to real application services/listeners, typed `VirtualThreadCommandEventBus<T>`, and MongoDB Testcontainers, verifying `POST /tasks` (`202`), `PUT /tasks/{taskId}` (`202`), `GET /tasks` (`200`), and `400` uniform `ErrorResponsePayload` contracts over real HTTP using **REST Assured**.
- `architecture.HexagonalArchitectureTest` & `architecture.TestBoundaryArchitectureTest` — verify generated OpenAPI stubs and `adapters.in.rest` stay strictly isolated from `domain`, `ports`, and `adapters.out`, and every `@RestController` has a matching HTTP API test.
