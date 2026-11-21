# Requirements: Task REST API

## Requirement 1: HTTP REST Endpoints
**User Story:** As a user, I want HTTP REST endpoints to create, update, and search tasks, so that external clients can manage tasks over HTTP.

1.1. The system shall specify HTTP REST endpoints for creating (`POST /tasks`), updating (`PUT /tasks/{taskId}`), and searching (`GET /tasks`) tasks in `src/api/task-api.yaml` and implement the generated server stubs in `adapters.in.rest` delegating to `ports.in`.
1.2. WHEN a valid HTTP REST request is received, the REST adapter shall translate payloads to `ports.in` commands/queries and return `202 Accepted` (`CommandAcceptedResponsePayload` with `commandId`, `timestamp`) for `POST`/`PUT`/`PATCH`/`DELETE` requests, and `200 OK` (`TasksResponsePayload` containing `tasks`, `count`, and query parameters) for `GET /tasks`.
1.3. IF input fails validation or a server error occurs, THEN the REST adapter shall return the corresponding `4xx` or `5xx` status code.
1.4. The REST adapter shall return all error responses using a uniform `ErrorResponsePayload` containing a list of `errors`, each with `errorCode`, `timestamp`, and `errorDescription`.
