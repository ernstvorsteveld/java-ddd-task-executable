# OpenAPI & REST Adapter Rules

- **Contract-First OpenAPI**: HTTP REST APIs are specified in `src/api/task-api.yaml` (OpenAPI 3.x).
- **Payload Naming Convention**:
  - Every request payload schema must end with `RequestPayload` (e.g., `CreateTaskRequestPayload`, `UpdateTaskRequestPayload`).
  - Every response payload schema must end with `ResponsePayload` (e.g., `TaskResponsePayload`, `ErrorResponsePayload`).
  - Collection/array responses must never be raw arrays: wrap them in a plural-named schema ending with `ResponsePayload` (e.g., `TasksResponsePayload`) that contains the resulting objects, the query parameters used, and the number of objects returned in the payload (`count`).
  - All error responses across all endpoints must use the uniform `ErrorResponsePayload` schema containing an `errors` list where each item has `errorCode`, `timestamp` (`date-time`), and `errorDescription`.
- **Command HTTP Responses (All Non-`GET` / Non-`OPTIONS` Endpoints)**:
  - All `POST`, `PUT`, `PATCH`, and `DELETE` endpoints must return `202 Accepted` with `CommandAcceptedResponsePayload` (`commandId`, `timestamp`) for valid input, or the corresponding `4xx` / `5xx` with `ErrorResponsePayload`.
- **Generated Server Stubs Outside `./src`**:
  - Generate Spring server stub interfaces (`interfaceOnly = true`) and transport DTOs using `org.openapi.generator` into `build/generated/openapi/src/main/java` (never under `./src`).
  - Generated packages:
    - `com.sternitc.task.api` (API interfaces)
    - `com.sternitc.task.api.model` (request/response DTOs)
- **Driving Adapter Implementation (`adapters.in`)**:
  - Hand-written REST controllers live under `src/main/java/com/sternitc/task/adapters/in/rest/` and implement the generated API interfaces.
  - Controllers act as an Anti-Corruption Layer (ACL): translate between generated REST DTOs and `domain.model` / `ports.in` commands & queries, then delegate to `ports.in`.
- **Strict Isolation**:
  - Generated OpenAPI classes and Spring framework types must never be imported by `domain` (`domain.model`, `domain.services`), `application`, `ports` (`ports.in`, `ports.out`), or `adapters.out`.
