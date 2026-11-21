# Adapters Layer Rules (`com.sternitc.task.adapters`)

## Structure

- `com.sternitc.task.adapters.in`: Driving adapters that receive external input (e.g., CLI, HTTP, events) and invoke `com.sternitc.task.ports.in.*`.
- `com.sternitc.task.adapters.out`: Driven adapters that implement `com.sternitc.task.ports.out.*` (e.g., in-memory stores, persistence, external clients).

## Dependency Rules

- `adapters.in` may depend on `ports.in` and `domain.model` types exposed on port signatures.
  - Forbidden: direct dependencies on `adapters.out`, `ports.out`, or `application` concrete classes (wire dependencies at the composition root).
- `adapters.out` implements `ports.out` and translates between external representations and `domain.model` types.
  - Forbidden: dependencies on `adapters.in`, `ports.in`, or `application`.

## Design Guidelines

- Keep adapters thin: perform translation/mapping and I/O only—never put domain business rules in adapters.
- Never leak external models or technical exceptions across port boundaries; translate them into domain types/exceptions at the adapter edge.
- For HTTP REST APIs, specify endpoints in `src/api/task-api.yaml`, generate server stub interfaces into `build/generated/openapi/src/main/java`, and implement them in `com.sternitc.task.adapters.in.rest`.
- Persistence adapters implement `ports.out` repositories in `com.sternitc.task.adapters.out.mongo` using MongoDB.
