# Design: Command Processing

## Domain Core (`com.sternitc.task.domain.model`)
- `CommandId(UUID value)` — non-null.
- `CommandReceipt(CommandId commandId, Instant timestamp)` — non-null.
- `enum CommandExecutionStatus { PENDING, COMPLETED, FAILED }`
- `sealed interface TaskCommandPayload permits TaskCommandPayload.PlanTask, TaskCommandPayload.UpdateTask` — domain-validated command payloads.
- `StoredCommand` (Aggregate Root) — identity on `CommandId`; holds `timestamp`, `TaskCommandPayload`, `CommandExecutionStatus`, optional `failureReason` (`Optional<String> failureReason()`); factories `accepted(id, timestamp, payload)` and `reconstitute(...)`; transitions `markCompleted()` and `markFailed(reason)`.
- `CommandStoredEvent(CommandId commandId, Instant occurredAt)` — past-tense domain event.

## Ports (`com.sternitc.task.ports`)
- `ports.in`:
  - `PlanTaskUseCase`: `CommandReceipt planTask(PlanTaskCommand command)`
  - `UpdateTaskUseCase`: `CommandReceipt updateTask(UpdateTaskCommand command)`
  - `CommandQuery`: `Optional<StoredCommand> findById(CommandId commandId)`
- `ports.out`:
  - `CommandRepository`: `StoredCommand save(StoredCommand command)`, `Optional<StoredCommand> findById(CommandId commandId)`
  - `CommandEventPublisher`: `void publish(CommandStoredEvent event)`
  - `CommandIdGenerator`: `CommandId nextId()`

## Application (`com.sternitc.task.application`)
- `PlanTaskService implements PlanTaskUseCase` & `UpdateTaskService implements UpdateTaskUseCase` — validate command via domain rules -> persist `StoredCommand.accepted(...)` via `CommandRepository` -> publish `CommandStoredEvent` via `CommandEventPublisher` -> return `CommandReceipt`.
- `CommandQueryService implements CommandQuery` — retrieves `StoredCommand` by `CommandId`.
- `CommandStoredEventHandler` — async `handle(CommandStoredEvent)`: loads `StoredCommand`, executes exhaustive `switch` on `TaskCommandPayload`, persists resulting `Task` via `TaskRepository`, updates `StoredCommand` to `COMPLETED` (or `FAILED` on domain/not-found error).

## Adapters (`com.sternitc.task.adapters.out`)
- `mongo/MongoCommandRepository implements CommandRepository` — stores BSON documents in MongoDB `commands` collection.
- `mongo/MongoTaskRepository implements TaskRepository` — stores BSON documents in MongoDB `tasks` collection (paginated `findByExecutor` & `search`).
- `async/VirtualThreadCommandEventBus implements CommandEventPublisher, AutoCloseable` — dispatches `CommandStoredEvent` asynchronously on Java 27 Virtual Threads.
- `UuidCommandIdGenerator implements CommandIdGenerator`.

## Testing
- `ports.in.TaskPlanningPortTest` — exercises `PlanTaskUseCase`, `UpdateTaskUseCase`, `TaskQuery`, and `CommandQuery` wired to real `MongoCommandRepository`, `MongoTaskRepository`, and `VirtualThreadCommandEventBus` backed by Testcontainers MongoDB (`mongo:8.0`) + Awaitility (zero mocks).
- `architecture.HexagonalArchitectureTest` — verifies MongoDB and Virtual Thread adapters stay isolated inside `adapters.out`.
